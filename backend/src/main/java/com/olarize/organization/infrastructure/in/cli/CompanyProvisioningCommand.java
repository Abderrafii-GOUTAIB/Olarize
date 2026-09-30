package com.olarize.organization.infrastructure.in.cli;

/** Process entry point for the restricted administrative command, separate from the web application. */
public final class CompanyProvisioningCommand {
    private CompanyProvisioningCommand() { }

    public static int run(String[] args) {
        return ProvisionCompanyCommand.run(args, System.in, System.out, System.err, System.getenv());
    }
}
