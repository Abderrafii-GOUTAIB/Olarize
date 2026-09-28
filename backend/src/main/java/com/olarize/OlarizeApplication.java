package com.olarize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OlarizeApplication {

	public static void main(String[] args) {
		if (args.length > 0 && "provision-company".equals(args[0])) {
            System.exit(com.olarize.organization.adapter.in.cli.CompanyProvisioningCommand.run(args));
            return;
        }
        if (args.length > 0 && "provision-membership".equals(args[0])) {
            System.exit(com.olarize.identity.adapter.in.cli.MembershipProvisioningCommand.run(args));
            return;
        }
        SpringApplication.run(OlarizeApplication.class, args);
	}

}
