package pt.ulisboa.tecnico.socialsoftware.tutor.submission.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Option
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.tutor.submission.dto.SubmissionDto
import pt.ulisboa.tecnico.socialsoftware.tutor.submission.domain.Submission
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User

@DataJpaTest
class UpdateSubmissionTest extends SpockTest{
    def student
    def teacher
    def question
    def optionOK
    def submission

    def setup() {
        student = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        student.setEnrolledCoursesAcronyms(courseExecution.getAcronym())
        userRepository.save(student)
        teacher = new User(USER_2_NAME, USER_2_USERNAME, User.Role.TEACHER)
        userRepository.save(teacher)
        question = new Question()
        question.setKey(1)
        question.setTitle(QUESTION_1_TITLE)
        question.setContent(QUESTION_1_CONTENT)
        question.setCourse(course)
        question.setStatus(Question.Status.IN_REVISION)
        questionRepository.save(question)
        optionOK = new Option()
        optionOK.setContent(OPTION_1_CONTENT)
        optionOK.setCorrect(true)
        optionOK.setSequence(0)
        optionOK.setQuestion(question)
        optionRepository.save(optionOK)
        submission = new Submission()
        submission.setQuestion(question)
        submission.setUser(student)
        submission.setCourseExecution(courseExecution)
        courseExecution.addSubmission(submission)
        student.addSubmission(submission)
        submissionRepository.save(submission)
    }

    def "edit a submission"(){
        given: "an edited questionDto"
        def questionDto = new QuestionDto(question)
        questionDto.setTitle(QUESTION_2_TITLE)
        questionDto.setContent(QUESTION_2_CONTENT)
        and: "a submissionDto"
        def submissionDto = new SubmissionDto(submission)
        submissionDto.setQuestion(questionDto)

        when:
        submissionService.updateSubmission(submission.getId(), submissionDto)

        then: "the submission is changed"
        submissionRepository.count() == 1L
        def result = submissionRepository.findAll().get(0)
        result.getId() != null
        result.getUser() == student
        result.getQuestion() != null
        result.getQuestion().getTitle() == questionDto.getTitle()
        result.getQuestion().getContent() == questionDto.getContent()
        result.getQuestion().getStatus() == Question.Status.IN_REVISION
        result.getCourseExecution() == courseExecution
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}




