package com.olarize.identity.adapter.in.cli;

/** Process entry point for the restricted membership command. */
public final class MembershipProvisioningCommand {
    private MembershipProvisioningCommand() { }
    public static int run(String[] args) {
        return ProvisionMembershipCommand.run(args, System.in, System.out, System.err, System.getenv());
    }
}
