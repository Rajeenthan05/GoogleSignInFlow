package com.rajee.auth2Peoject.security;
package com.rajee.auth2Peoject.service;

import com.rajee.auth2Peoject.enums.AuthProvider;
import com.rajee.auth2Peoject.model.User;
import com.rajee.auth2Peoject.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUserLocal(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAuthProvide(AuthProvider.LOCAL);
        return  usersRepository.save(user);

    }

    public User loginUserLocal(User user){
        User existingUser = usersRepository.findByEmail(user.getEmail()).orElse(null);
        if (existingUser != null){
            if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
                throw new RuntimeException("User pasowrd does  ot match");
            }
            return existingUser;
        }
        throw new RuntimeException("User not found");
    }

    public  User loginRegisterByGoogleOAuth2(OAuth2AuthenticationToken auth2AuthenticationToken){

        OAuth2User oAuth2User = auth2AuthenticationToken.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("USER Email FROM GOOGLE  IS {}",email );
        log.info("USER Name from GOOGLE IS {}",name );

        User user = usersRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setAuthProvide(AuthProvider.GOOGLE);
            return usersRepository.save(user);
        }
        return user;
    }
}

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;




@Configuration
@EnableWebSecurity
public class SecurityConfig {


   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
       httpSecurity
               .csrf(AbstractHttpConfigurer::disable)
               .cors(Customizer.withDefaults())
               .authorizeHttpRequests(requests -> requests
                       .requestMatchers("/register", "/login/**", "/logout").permitAll()
                       .anyRequest().authenticated())
               .oauth2Login(oauth2 -> oauth2
                       .loginPage("/login/google")
                       .defaultSuccessUrl("/loginSuccess", true)
                       .failureUrl("/loginFailure"))
               .logout(logout -> logout
                       .logoutUrl("/logout")
                       .logoutSuccessUrl("/login/local") // Redirect to homepage after logout
                       .invalidateHttpSession(true)
                       .clearAuthentication(true)
                       .deleteCookies("JSESSIONID")
                       .permitAll());


       return httpSecurity.build();
   }




   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();  // This creates the BCryptPasswordEncoder bean
   }


}


