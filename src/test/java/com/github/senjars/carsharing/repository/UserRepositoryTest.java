package com.github.senjars.carsharing.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.senjars.carsharing.model.user.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should successfully retrieve a user by their unique email address")
    void findByEmail_validEmail_returnsUser() {
         // GIVEN
         String email = "example@test.com";
         User user = createUser(email);
         userRepository.save(user);

         // WHEN
         Optional<User> foundUser = userRepository.findByEmail(email);

         // THEN
         assertThat(foundUser).isPresent();
         assertThat(foundUser.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Should return an empty optional when searching for a non-existent email")
    void findByEmail_invalidEmail_returnsEmpty() {
         // GIVEN
         String invalidEmail = "i_dont_exist@test.com";

         // WHEN
         Optional<User> foundUser = userRepository.findByEmail(invalidEmail);

         // THEN
         assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should return true when checking existence of an already registered email")
    void existsByEmail_validEmail_returnsTrue() {
        // GIVEN
        String email = "example@test.com";
        User user = createUser(email);
        userRepository.save(user);

        // WHEN
        boolean exists = userRepository.existsByEmail(email);

        // THEN
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when checking existence of an unregistered email")
    void existsByEmail_invalidEmail_returnsFalse() {
        // GIVEN
        String invalidEmail = "i_dont_exist@test.com";

        // WHEN
        boolean exists = userRepository.existsByEmail(invalidEmail);

        // THEN
        assertThat(exists).isFalse();
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }
}
