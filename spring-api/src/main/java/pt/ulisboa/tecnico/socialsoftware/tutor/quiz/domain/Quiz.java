package pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain;

import pt.ulisboa.tecnico.socialsoftware.tutor.answer.domain.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.dto.QuizDto;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "quizzes",
        indexes = {
                @Index(name = "quizzes_indx_0", columnList = "number")
        })
public class Quiz implements Serializable {
   public enum QuizType {
       EXAM, TEST, STUDENT, TEACHER
   }

   @Id
   @GeneratedValue(strategy=GenerationType.IDENTITY)
   private Integer id;

   @Column(name = "number")
   private Integer number;

   private String title;

   @Column(name = "generation_date")
   private LocalDateTime date;

   @Column(name = "available_date")
   private LocalDateTime availableDate;

   private Integer year;
   private String type;
   private Integer series;
   private String version;

   @OneToMany(cascade = CascadeType.ALL, mappedBy = "quiz")
   private Set<QuizQuestion> quizQuestions = new HashSet<>();

   @OneToMany(cascade = CascadeType.ALL, mappedBy = "quiz")
   private Set<QuizAnswer> quizAnswers = new HashSet<>();

   public Quiz() {}

   public Quiz(QuizDto quiz) {
       checkQuestions(quiz.getQuestions());

       this.number = quiz.getNumber();

       setTitle(quiz.getTitle());
       this.date = quiz.getDate();
       this.type = quiz.getType();
       if (this.type.equals(QuizType.STUDENT.name())) {
           this.availableDate = this.date;
       } else {
           setAvailableDate(quiz.getAvailableDate());
       }
       this.year = quiz.getYear();
       this.series = quiz.getSeries();
       this.version = quiz.getVersion();
   }

     private void checkTitle(String title) {
        if (title == null || title.trim().length() == 0) {
            throw new TutorException(TutorException.ExceptionError.QUIZ_NOT_CONSISTENT, "Title");
        }
    }

    private void checkAvailableDate(LocalDateTime availableDate) {
        if (this.type.equals(QuizType.TEACHER.name()) && availableDate == null) {
            throw new TutorException(TutorException.ExceptionError.QUIZ_NOT_CONSISTENT, "Available date");
        }
    }

    private void checkQuestions(List<QuestionDto> questions) {
        if (questions != null) {
            for (QuestionDto questionDto : questions) {
                if (questionDto.getSequence() != questions.indexOf(questionDto) + 1) {
                    throw new TutorException(TutorException.ExceptionError.QUIZ_NOT_CONSISTENT, "sequence of questions not correct");
                }
            }
        }
    }

     public void remove() {
       getQuizQuestions().stream().collect(Collectors.toList()).forEach(QuizQuestion::remove);

       quizQuestions.clear();
    }

    public void checkCanRemove() {
       if (quizAnswers.size() != 0) {
           throw new TutorException(TutorException.ExceptionError.QUIZ_HAS_ANSWERS, String.valueOf(quizAnswers.size()));
       }

       getQuizQuestions().forEach(QuizQuestion::checkCanRemove);
    }

    public void generate(int quizSize, List<Question> activeQuestions) {
       Random rand = new Random();
       int numberOfActiveQuestions = activeQuestions.size();
       Set <Integer> usedQuestions = new HashSet<>();

       int numberOfQuestions = 0;
       while (numberOfQuestions < quizSize) {
           int next = rand.nextInt(numberOfActiveQuestions);
           if(!usedQuestions.contains(next)) {
               usedQuestions.add(next);
               new QuizQuestion(this, activeQuestions.get(next), numberOfQuestions++);
           }
       }

       this.setDate(LocalDateTime.now());
       this.setType(QuizType.STUDENT.name());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumber() {
       return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
       checkTitle(title);
        this.title = title;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
       checkAvailableDate(availableDate);
        this.availableDate = availableDate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }

    public void setQuizQuestions(Set<QuizQuestion> quizQuestions) {
        this.quizQuestions = quizQuestions;
    }

    public Set<QuizAnswer> getQuizAnswers() {
        return quizAnswers;
    }

    public void setQuizAnswers(Set<QuizAnswer> quizAnswers) {
        this.quizAnswers = quizAnswers;
    }

    public void addQuizQuestion(QuizQuestion quizQuestion) {
        if (quizQuestions == null) {
            quizQuestions = new HashSet<>();
        }
        this.quizQuestions.add(quizQuestion);
    }

    public void addQuizAnswer(QuizAnswer quizAnswer) {
        if (quizAnswers == null) {
            quizAnswers = new HashSet<>();
        }
        this.quizAnswers.add(quizAnswer);
    }
}