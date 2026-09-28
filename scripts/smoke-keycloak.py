"""Exercise local Keycloak PKCE login and /api/v1/identity with a temporary user."""

import base64
import hashlib
import http.cookiejar
import html.parser
import json
import os
import secrets
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

TIMEOUT_SECONDS = 20
REDIRECT_URI = os.environ.get("SMOKE_REDIRECT_URI", "http://localhost:5173/auth/callback")


def load_settings():
    env_file = Path(__file__).resolve().parents[1] / ".env"
    settings = {}
    for line in env_file.read_text().splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            settings[key] = value
    return settings


def post_form(url, values):
    request = urllib.request.Request(
        url, data=urllib.parse.urlencode(values).encode("utf-8")
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        return json.load(response)


class StopAtFrontend(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, url):
        if url.startswith(REDIRECT_URI):
            return None
        return super().redirect_request(request, response, code, message, headers, url)


class LoginFormParser(html.parser.HTMLParser):
    def __init__(self):
        super().__init__()
        self.action = None

    def handle_starttag(self, tag, attributes):
        attributes = dict(attributes)
        if tag == "form" and attributes.get("id") == "kc-form-login":
            self.action = attributes["action"]


def create_temporary_user(base_url, admin_headers):
    username = "smoke-" + secrets.token_hex(6)
    password = secrets.token_urlsafe(24)
    user = {
        "username": username,
        "enabled": True,
        "email": username + "@example.test",
        "emailVerified": True,
        "firstName": "Synthetic",
        "lastName": "Test",
        "credentials": [{"type": "password", "value": password, "temporary": False}],
    }
    request = urllib.request.Request(
        base_url + "/admin/realms/olarize/users",
        data=json.dumps(user).encode("utf-8"),
        headers=admin_headers,
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        user_url = response.headers["Location"]
    return username, password, user_url


def login_with_pkce(base_url, username, password):
    jar = http.cookiejar.CookieJar()
    browser = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar), StopAtFrontend()
    )
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(
        hashlib.sha256(verifier.encode("ascii")).digest()
    ).rstrip(b"=").decode("ascii")
    state = secrets.token_urlsafe(16)
    parameters = {
        "client_id": "olarize-web",
        "redirect_uri": REDIRECT_URI,
        "response_type": "code",
        "scope": "openid profile.read",
        "state": state,
        "nonce": secrets.token_urlsafe(16),
        "code_challenge": challenge,
        "code_challenge_method": "S256",
    }
    authorize = base_url + "/realms/olarize/protocol/openid-connect/auth"
    with browser.open(authorize + "?" + urllib.parse.urlencode(parameters),
                      timeout=TIMEOUT_SECONDS) as response:
        page = response.read().decode("utf-8")
    form = LoginFormParser()
    form.feed(page)
    assert form.action, "Keycloak login form is missing"

    # Browsers treat localhost as a secure context; urllib's cookie jar does not.
    for cookie in jar:
        cookie.secure = False

    request = urllib.request.Request(
        form.action,
        data=urllib.parse.urlencode({"username": username, "password": password}).encode("utf-8"),
    )
    try:
        browser.open(request, timeout=TIMEOUT_SECONDS)
        raise AssertionError("Keycloak did not redirect after login")
    except urllib.error.HTTPError as response:
        assert response.code in (302, 303), "Keycloak rejected the local login"
        callback = urllib.parse.parse_qs(
            urllib.parse.urlparse(response.headers["Location"]).query
        )
    assert callback["state"] == [state], "Authorization state does not match"
    return post_form(base_url + "/realms/olarize/protocol/openid-connect/token", {
        "grant_type": "authorization_code",
        "client_id": "olarize-web",
        "code": callback["code"][0],
        "redirect_uri": REDIRECT_URI,
        "code_verifier": verifier,
    })


def check_endpoint(api_url, tokens, subject):
    endpoint = api_url + "/api/v1/identity"
    access_request = urllib.request.Request(
        endpoint, headers={"Authorization": "Bearer " + tokens["access_token"]}
    )
    with urllib.request.urlopen(access_request, timeout=TIMEOUT_SECONDS) as response:
        assert response.status == 200
        assert json.load(response) == {"subject": subject}
    print("PASS: real Keycloak authorization-code + PKCE login and /api/v1/identity")

    id_request = urllib.request.Request(
        endpoint, headers={"Authorization": "Bearer " + tokens["id_token"]}
    )
    try:
        urllib.request.urlopen(id_request, timeout=TIMEOUT_SECONDS)
        raise AssertionError("ID token was accepted as an API access token")
    except urllib.error.HTTPError as response:
        assert response.code == 401
    print("PASS: real Keycloak ID token rejected")


def main():
    settings = load_settings()
    base_url = "http://localhost:" + settings.get("KEYCLOAK_PORT", "8081")
    api_url = "http://localhost:" + settings.get("BACKEND_PORT", "8080")
    admin = post_form(base_url + "/realms/master/protocol/openid-connect/token", {
        "grant_type": "password",
        "client_id": "admin-cli",
        "username": "admin",
        "password": settings["KEYCLOAK_ADMIN_PASSWORD"],
    })["access_token"]
    headers = {"Authorization": "Bearer " + admin, "Content-Type": "application/json"}
    username, password, user_url = create_temporary_user(base_url, headers)
    try:
        tokens = login_with_pkce(base_url, username, password)
        check_endpoint(api_url, tokens, user_url.rsplit("/", 1)[-1])
    finally:
        request = urllib.request.Request(user_url, headers=headers, method="DELETE")
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS):
            pass
        print("Temporary synthetic user removed")


if __name__ == "__main__":
    main()
