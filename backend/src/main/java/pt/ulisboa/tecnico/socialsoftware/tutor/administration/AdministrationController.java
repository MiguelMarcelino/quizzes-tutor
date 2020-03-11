package pt.ulisboa.tecnico.socialsoftware.tutor.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.AUTHENTICATION_ERROR;

@RestController
public class AdministrationController {

    @Autowired
    private AdministrationService administrationService;

    @GetMapping("/admin/courses/executions")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_DEMO_ADMIN')")
    public List<CourseDto> getCourseExecutions(Principal principal) {
        User user = (User) ((Authentication) principal).getPrincipal();

        if(user == null){
            throw new TutorException(AUTHENTICATION_ERROR);
        }
        return administrationService.getCourseExecutions(user.getRole());
    }

    @PostMapping("/admin/courses/executions")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#courseDto, 'DEMO.ACCESS'))")
    public CourseDto createCourseExecution(@RequestBody CourseDto courseDto) {
        return administrationService.createExternalCourseExecution(courseDto);
    }

    @DeleteMapping("/admin/courses/executions/{executionCourseId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#executionCourseId, 'DEMO.ACCESS'))")
    public ResponseEntity removeCourseExecution(@PathVariable Integer executionCourseId) {
        administrationService.removeCourseExecution(executionCourseId);

        return ResponseEntity.ok().build();
    }


}