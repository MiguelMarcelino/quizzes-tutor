package pt.ulisboa.tecnico.socialsoftware.tutor.auth.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.auth.FenixEduInterface
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseDto
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User

import java.util.stream.Collectors

@DataJpaTest
class FenixAuthTest extends SpockTest {
    def client
    def courses
    def existingUsers
    def existingCourses
    def existingCourseExecutions

    def setup() {
        client = Mock(FenixEduInterface)

        courses = new ArrayList<>()
        def courseDto = new CourseDto(COURSE_1_NAME, COURSE_1_ACRONYM, COURSE_1_ACADEMIC_TERM)
        courses.add(courseDto)
        courseDto = new CourseDto("Tópicos Avançados em Engenharia de Software", "TAES", COURSE_1_ACADEMIC_TERM)
        courses.add(courseDto)

        existingUsers = userRepository.findAll().size()
        existingCourses = courseRepository.findAll().size()
        existingCourseExecutions = courseExecutionRepository.findAll().size()
    }

    def "no teacher has courses create teacher"() {
        given:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> courses

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null) != null
        and: 'no courses are created'
        courseRepository.findAll().size() == existingCourses
        courseExecutionRepository.findAll().size() == existingCourseExecutions
    }

    def "no teacher has course and is in database, then create and add"() {
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> courses

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        def user = userRepository.findByUsername(USER_1_USERNAME).orElse(null)
        user != null
        and: 'is teaching'
        user.getCourseExecutions().size() == 1
        user.getCourseExecutions().stream().collect(Collectors.toList()).get(0).getAcronym() == COURSE_1_ACRONYM
    }

    def "no teacher does not have courses throw exception"() {
        given:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        authService.fenixAuth(client)

        then:
        thrown(TutorException)
        and: 'the user is not created db'
        userRepository.findAll().size() == existingUsers
        and: 'no courses are created'
        courseRepository.findAll().size() == existingCourses
        courseExecutionRepository.findAll().size() == existingCourseExecutions
    }

    def "teacher has courses"() {
        given: 'a teacher'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.TEACHER)
        userRepository.save(user)
        user.setKey(user.getId())

        and:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> courses

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null) != null
        and: 'no courses are created'
        courseRepository.findAll().size() == existingCourses
        courseExecutionRepository.findAll().size() == existingCourseExecutions
    }

    def "teacher does not have courses"() {
        given: 'a teacher'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.TEACHER)
        userRepository.save(user)
        user.setKey(user.getId())

        and:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null) != null
        and: 'no courses are created'
        courseRepository.findAll().size() == existingCourses
        courseExecutionRepository.findAll().size() == existingCourseExecutions
    }

    def "teacher has course and is in database, then add"() {
        given: 'a teacher'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.TEACHER)
        userRepository.save(user)
        user.setKey(user.getId())

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> courses

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        def user2 = userRepository.findByUsername(USER_1_USERNAME).orElse(null)
        user2 != null
        and: 'is teaching'
        user2.getCourseExecutions().size() == 1
        user2.getCourseExecutions().stream().collect(Collectors.toList()).get(0).getAcronym() == COURSE_1_ACRONYM
    }

    def "no student no courses, throw exception"() {
        given:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        authService.fenixAuth(client)

        then:
        thrown(TutorException)
        and: 'the user is not created in the db'
        userRepository.findAll().size() == existingUsers
        and: 'no courses are created'
        courseRepository.findAll().size() == existingCourses
        courseExecutionRepository.findAll().size() == existingCourseExecutions
    }

    def "no student has courses but not in database, throw exception"() {
        userRepository.deleteAll()
        courseExecutionRepository.deleteAll()
        courseRepository.deleteAll()

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        authService.fenixAuth(client)

        then:
        thrown(TutorException)
        and: 'the user is not created in the db'
        userRepository.findAll().size() == 0
        and: 'no courses are created'
        courseRepository.findAll().size() == 0
        courseExecutionRepository.findAll().size() == 0
    }

    def "no student has courses and in database, create student with attending course"() {
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        User user = userRepository.findByUsername(USER_1_USERNAME).orElse(null)
        user.getRole() == User.Role.STUDENT
        and: 'is enrolled'
        user.getCourseExecutions().size() == 1
        user.getCourseExecutions().stream().collect(Collectors.toList()).get(0).getAcronym() == COURSE_1_ACRONYM
    }

    def "student does not have courses, throw exception"() {
        given: 'a student'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        userRepository.save(user)
        user.setKey(user.getId())

        and:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        authService.fenixAuth(client)

        then: "the returned data are correct"
        thrown(TutorException)
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        and: 'is not enrolled'
        user.getCourseExecutions().size() == 0
    }

    def "student has courses but not in the database, throw exception"() {
        userRepository.deleteAll()
        courseExecutionRepository.deleteAll()
        courseRepository.deleteAll()

        given: 'a student'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        userRepository.save(user)
        user.setKey(user.getId())

        and:
        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        authService.fenixAuth(client)

        then: "the returned data are correct"
        thrown(TutorException)
        and: 'the user is created in the db'
        userRepository.findAll().size() == 1
        and: 'is not enrolled'
        user.getCourseExecutions().size() == 0
    }

    def "student has courses and in the database, add course"() {
        given: 'a student'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        userRepository.save(user)
        user.setKey(user.getId())

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null).getRole() == User.Role.STUDENT
        and: 'is enrolled'
        user.getCourseExecutions().size() == 1
        user.getCourseExecutions().stream().collect(Collectors.toList()).get(0).getAcronym() == COURSE_1_ACRONYM
    }

    def "student has teaching courses, throw exception"() {
        given: 'a student'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        userRepository.save(user)
        user.setKey(user.getId())

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> new ArrayList<>()
        client.getPersonTeachingCourses() >> courses

        when:
        authService.fenixAuth(client)

        then:
        thrown(TutorException)
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null).getRole() == User.Role.STUDENT
        and: 'is not enrolled'
        user.getCourseExecutions().size() == 0
    }

    def "teacher has attending courses, does not add course"() {
        given: 'a teacher'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.TEACHER)
        userRepository.save(user)
        user.setKey(user.getId())

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> new ArrayList<>()

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null).getRole() == User.Role.TEACHER
        and: 'is enrolled'
        user.getCourseExecutions().size() == 0
    }

    def "student has attending and teaching courses, add attending course"() {
        given: 'a teacher'
        def user = new User(USER_1_NAME, USER_1_USERNAME, User.Role.TEACHER)
        userRepository.save(user)
        user.setKey(user.getId())

        client.getPersonName() >> USER_1_NAME
        client.getPersonUsername() >> USER_1_USERNAME
        client.getPersonAttendingCourses() >> courses
        client.getPersonTeachingCourses() >> courses

        when:
        def result = authService.fenixAuth(client)

        then: "the returned data are correct"
        result.user.username == USER_1_USERNAME
        result.user.name == USER_1_NAME
        and: 'the user is created in the db'
        userRepository.findAll().size() == existingUsers + 1
        userRepository.findByUsername(USER_1_USERNAME).orElse(null).getRole() == User.Role.TEACHER
        and: 'is enrolled'
        user.getCourseExecutions().size() == 1
        user.getCourseExecutions().stream().collect(Collectors.toList()).get(0).getAcronym() == COURSE_1_ACRONYM
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
