package pt.ulisboa.tecnico.socialsoftware.tutor.impexp.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.domain.QuestionAnswer
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.domain.QuizAnswer
import pt.ulisboa.tecnico.socialsoftware.tutor.config.DateHandler
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Option
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.QuizQuestion
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User

@DataJpaTest
class ImportExportAnswersTest extends SpockTest {
    def quizAnswer
    def questionAnswer

    def setup() {
        Question question = new Question()
        question.setCourse(course)
        question.setKey(1)
        question.setTitle(QUESTION_1_TITLE)
        question.setContent(QUESTION_1_CONTENT)
        question.setStatus(Question.Status.AVAILABLE)
        questionRepository.save(question)

        Option option = new Option()
        option.setContent(OPTION_1_CONTENT)
        option.setCorrect(true)
        option.setSequence(0)
        option.setQuestion(question)
        optionRepository.save(option)

        Quiz quiz = new Quiz()
        quiz.setKey(1)
        quiz.setTitle(QUIZ_TITLE)
        quiz.setType(Quiz.QuizType.GENERATED.toString())
        quiz.setCourseExecution(courseExecution)

        quiz.setCreationDate(DateHandler.now())
        quiz.setAvailableDate(DateHandler.now())
        quiz.setConclusionDate(DateHandler.now())
        quiz.setType(Quiz.QuizType.EXAM.toString())
        quizRepository.save(quiz)

        QuizQuestion quizQuestion= new QuizQuestion()
        quizQuestion.setSequence(0)
        quizQuestion.setQuiz(quiz)
        quizQuestion.setQuestion(question)
        quizQuestionRepository.save(quizQuestion)

        User user = userService.createUser(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)

        quizAnswer = new QuizAnswer()
        quizAnswer.setQuiz(quiz)
        quizAnswer.setUser(user)
        quizAnswer.setAnswerDate(LOCAL_DATE_TODAY)
        quizAnswer.setCompleted(true)
        quizAnswerRepository.save(quizAnswer)

        questionAnswer = new QuestionAnswer()
        questionAnswer.setQuizAnswer(quizAnswer)
        questionAnswer.setTimeTaken(1)
        questionAnswer.setOption(option)
        questionAnswer.setSequence(0)
        questionAnswer.setQuizQuestion(quizQuestion)
        questionAnswerRepository.save(questionAnswer)
    }

    def 'export and import answers'() {
        given: 'a xml with a quiz'
        def answersXml = answerService.exportAnswers()
        and: 'delete answers'
        answerService.deleteQuizAnswer(quizAnswer)

        when:
        answerService.importAnswers(answersXml)

        then:
        quizAnswerRepository.findAll().size() == 1
        def quizAnswerResult = quizAnswerRepository.findAll().get(0)
        quizAnswerResult.getAnswerDate() == LOCAL_DATE_TODAY
        quizAnswerResult.isCompleted()
        quizAnswerResult.getUser().getName() == USER_1_NAME
        quizAnswerResult.getUser().getUsername() == USER_1_USERNAME
        quizAnswerResult.getQuiz().getKey() == 1
        questionAnswerRepository.findAll().size() == 1
        def questionAnswerResult = questionAnswerRepository.findAll().get(0)
        questionAnswerResult.getTimeTaken() == 1
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
