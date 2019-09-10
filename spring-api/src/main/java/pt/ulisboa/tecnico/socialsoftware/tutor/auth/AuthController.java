package pt.ulisboa.tecnico.socialsoftware.tutor.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.sdk.ApplicationConfiguration;
import org.fenixedu.sdk.FenixEduClientImpl;
import org.fenixedu.sdk.FenixEduUserDetails;
import org.fenixedu.sdk.exception.FenixEduClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.InvalidFenixException;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.NotEnrolledException;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.domain.User;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.service.UserService;

import java.util.Arrays;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException.ExceptionError.FENIX_ERROR;

@RestController
@RequestMapping("/auth")
public class AuthController {
        private static Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static String COURSE_ACRONYM = "ASof";
    private static String[] ADMINS = {"ist181002", "ist12628"};

    @Autowired
    private UserService userService;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${oauth.consumer.key}")
    private String oauthConsumerKey;

    @Value("${oauth.consumer.secret}")
    private String oauthConsumerSecret;

    @Value("${callback.url}")
    private String callbackUrl;

    @PostMapping("/fenix")
    public ResponseEntity<?> fenixAuth(@RequestBody FenixCode data) {

        ApplicationConfiguration config = new ApplicationConfiguration(baseUrl, oauthConsumerKey, oauthConsumerSecret, callbackUrl);
        FenixEduClientImpl client;
        FenixEduUserDetails userDetails;

        try {
            client = new FenixEduClientImpl(config);
        } catch (FenixEduClientException e) {
            throw new InvalidFenixException("Wrong server configuration files");
        }

        // Get user's authorization data (access_token and refresh_token) client.
        try {
            logger.debug("fenixAuth");
            userDetails = client.getUserDetailsFromCode(data.getCode());
        } catch (FenixEduClientException e) {
            throw new InvalidFenixException("Wrong user Fenix code");
        } catch (Exception e) {
            e.printStackTrace();
            throw new TutorException(FENIX_ERROR, "Wrong configuration");
        }


        // When requesting user's private data, the authorization object must be passed along.
        JsonObject person = client.getPerson(userDetails.getAuthorization());
        String username = person.get("username").toString().replaceAll("^\"|\"$", "");

        // Find if user is in database
        User user = this.userService.findByUsername(username);

        // If user is not in database
        if (user == null){
            // Verify if user is attending the course
            JsonArray courses = client.getPersonCourses(userDetails.getAuthorization()).get("attending").getAsJsonArray();

            boolean isStudent = false;
            for (JsonElement course : courses) {
                isStudent |= course.getAsJsonObject().get("acronym").getAsString().equals(COURSE_ACRONYM);
            }

            if ( Arrays.stream(ADMINS).anyMatch(username::equals) ){
                user = this.userService.create(person.get("name").toString().replaceAll("^\"|\"$", ""), username, User.Role.ADMIN);
            } else if (username.equals("ist12628")) {
                user = this.userService.create(person.get("name").toString().replaceAll("^\"|\"$", ""), username, User.Role.ADMIN);
            } else if (isStudent) {
                user = this.userService.create(person.get("name").toString().replaceAll("^\"|\"$", ""), username, User.Role.STUDENT);
            } else {
                // Verify if user is teaching the course
                courses = client.getPersonCourses(userDetails.getAuthorization()).get("teaching").getAsJsonArray();

                boolean isTeacher = false;
                for (JsonElement course : courses) {
                    isTeacher |= course.getAsJsonObject().get("acronym").getAsString().equals(COURSE_ACRONYM);
                }

                if (isTeacher) {
                    user = this.userService.create(person.get("name").toString().replaceAll("^\"|\"$", ""), username, User.Role.TEACHER);
                } else {
                    throw new NotEnrolledException("User " + username + " is not enrolled");
                }
            }
        }

        String token = JwtTokenProvider.generateToken(user);
        return ResponseEntity.ok(new JwtAuthenticationResponse(token, user.getRole()));

    }

}