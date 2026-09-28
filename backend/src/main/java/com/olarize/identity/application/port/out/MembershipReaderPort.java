package com.olarize.identity.application.port.out;

import com.olarize.identity.domain.model.Membership;
import java.util.List;

public interface MembershipReaderPort {
    List<Membership> activeFor(String authenticatedSubject);
}
