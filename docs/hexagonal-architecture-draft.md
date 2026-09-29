# Draft: Olarize features and hexagonal layers

This is a visual guide to the backend as it exists today. Olarize is one Spring Boot application with two implemented business features, `identity` and `organization`. Each feature owns its domain, application logic and adapters. `platform` supplies shared security, API errors and request IDs. Employee records, leave, payroll and the other planned features are not implemented yet.

## Features first, layers inside each feature

```mermaid
flowchart TB
    Caller[Swagger or API client]
    Operator[Restricted operator CLI]
    Database[(PostgreSQL)]
    Keycloak[Keycloak]

    subgraph App[Olarize Spring Boot application]
        Platform[platform<br/>security · API errors · request IDs]

        subgraph Identity[identity feature]
            IdentityIn[adapter/in<br/>web controllers · security · CLI]
            IdentityApp[application<br/>ports · services]
            IdentityDomain[domain<br/>membership models · rules]
            IdentityOut[adapter/out<br/>JDBC repositories · Keycloak adapter · token signer]
            IdentityIn --> IdentityApp
            IdentityApp --> IdentityDomain
            IdentityApp --> IdentityOut
        end

        subgraph Organization[organization feature]
            OrganizationIn[adapter/in<br/>provision-company CLI]
            OrganizationApp[application<br/>ports · ProvisionCompanyService]
            OrganizationDomain[domain<br/>company registration rules]
            OrganizationOut[adapter/out<br/>company JDBC repositories]
            OrganizationIn --> OrganizationApp
            OrganizationApp --> OrganizationDomain
            OrganizationApp --> OrganizationOut
        end
    end

    Caller --> Platform --> IdentityIn
    Operator --> OrganizationIn
    IdentityOut --> Database
    IdentityOut --> Keycloak
    OrganizationOut --> Database
```

The arrows above show how a request travels. Each feature keeps its code together; there is no single project-wide `controllers/`, `services/` or `repositories/` folder.

## Zoom in: changing a company membership

```mermaid
flowchart LR
    Request[HTTP request] --> Controller[adapter/in/web<br/>MembershipAdministrationController]

    subgraph Identity[identity feature]
        Controller --> InPort[application/port/in<br/>MembershipAdministrationUseCase]
        InPort --> Service{{application/service<br/>MembershipAdministrationService}}
        Service --> Rules[domain/model<br/>MembershipAdministrationRules]
        Service --> OutPort[application/port/out<br/>MembershipAdministrationPort]
        Service --> VerifyPort[application/port/out<br/>IdentitySubjectVerificationPort]
        Repository[adapter/out/persistence<br/>MembershipAdministrationJdbcRepository] -. implements .-> OutPort
        KeycloakAdapter[adapter/out/external<br/>KeycloakSubjectVerificationAdapter] -. implements .-> VerifyPort
    end

    Repository --> Database[(PostgreSQL)]
    KeycloakAdapter --> Keycloak[Keycloak admin API]
```

The **incoming adapter** translates HTTP into a use-case call. The **incoming port** says what the application offers. The **service** runs the membership rules. The **outgoing ports** say what the service needs from storage and Keycloak. The **outgoing adapters** provide those capabilities. Dotted arrows mean “implements this port.” In source code, the service depends on port interfaces; dependency injection connects it to the adapters.

For this feature, `MembershipJdbcMapper` converts SQL rows to domain `Membership` objects. Request and response DTOs live beside the controller in `adapter/in/web`; the repository and mapper live in `adapter/out/persistence`. The current persistence implementation uses JDBC, so these files have `JdbcRepository` and `JdbcMapper` suffixes. There are no JPA entities or `JpaRepository` interfaces yet.

## Folder shape

```text
com/olarize/
├── identity/
│   ├── domain/
│   ├── application/
│   │   ├── port/in/      use cases called by controllers
│   │   ├── port/out/     contracts needed by services
│   │   └── service/      business workflow
│   └── adapter/
│       ├── in/           web, security and CLI entry points
│       └── out/          JDBC, Keycloak and token implementations
├── organization/
│   ├── domain/
│   ├── application/
│   └── adapter/
└── platform/             shared technical infrastructure
```

Some subfolders are absent until real code needs them. This diagram is a draft explanation of the current backend, not a proposal to create empty packages.
