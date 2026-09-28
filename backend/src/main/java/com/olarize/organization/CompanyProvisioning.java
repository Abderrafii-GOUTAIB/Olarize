package com.olarize.organization;

import com.olarize.organization.internal.adapter.in.cli.ProvisionCompanyCommand;

/** Process entry point for the restricted administrative command, separate from the web application. */
public final class CompanyProvisioning {
    private CompanyProvisioning() { }

    public static int run(String[] args) {
        return ProvisionCompanyCommand.run(args, System.in, System.out, System.err, System.getenv());
    }
}
