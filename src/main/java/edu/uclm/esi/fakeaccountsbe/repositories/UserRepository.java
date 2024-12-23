package edu.uclm.esi.fakeaccountsbe.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.fakeaccountsbe.model.User;

public interface UserRepository extends JpaRepository<User, String> {
}
