package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.MongoTransactionManager;

import telran.exceptions.NotFoundException;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.model.StudentDoc;
import telran.students.service.StudentsService;

@SpringBootTest
//@TestInstance(Lifecycle.PER_CLASS)
class StudentsServiceTests {
	@Autowired
	StudentsService studentsService;
	@Autowired
	DbTestCreation dbCreation;
	@MockBean
	MongoTransactionManager mongoTransactionManager;
	
	final private long notExistedId = 999;
	private Student notExistedStudent;
	private Student existedStudent;
		
	@BeforeEach
	void setUp() {
		dbCreation.createDB();
		
		notExistedStudent = new Student(notExistedId, "Vasya", "0555555555");
		existedStudent = dbCreation.students[0];
	}
	@Test
	void getMarksTest() {
		Mark[] marksActual = studentsService.getMarks(1).toArray(Mark[]::new);
		Mark[] marksExpected = dbCreation.getStudentMarks(1);
		assertArrayEquals(marksExpected, marksActual);
	}

	@Test
	@DisplayName("add student test")	
	void addStudentTest() {
		
		Student returnedStudent = studentsService.addStudent(notExistedStudent);
		Student savedStudent = studentsService.getStudent(notExistedId);
		assertEquals(notExistedStudent, returnedStudent);
		assertEquals(notExistedStudent, savedStudent);
		
		
		assertThrowsExactly(IllegalStateException.class, () -> studentsService.addStudent(existedStudent));

	}

	@Test
	@DisplayName("get student by id test")
	void getStudentTest() {
		assertEquals(existedStudent, studentsService.getStudent(existedStudent.id()));
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getStudent(notExistedId));

	}

	@Test
	@DisplayName("update phone test")
	void updatePhoneTest() {
	
		String newPhoneNumber = "999999999";
		assertNotEquals(existedStudent.phone(), newPhoneNumber);
		
		Student updatedStudentFromMethod = studentsService.updatePhone(existedStudent.id(), newPhoneNumber);
		Student updatedStudentFromDB = studentsService.getStudent(existedStudent.id());
		
		StudentDoc existedStudentDoc = StudentDoc.of(existedStudent);
		existedStudentDoc.setPhone(newPhoneNumber);
		Student updatedExistedStudent = existedStudentDoc.build();
		
		assertEquals(updatedExistedStudent, updatedStudentFromMethod);
		assertEquals(updatedExistedStudent, updatedStudentFromDB);
		
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.updatePhone(notExistedStudent.id(), newPhoneNumber));
		
	}
	
	@Test
	@DisplayName("add mark test")
	void addMarkTest() {
	
		Mark newMark = new Mark("subject5", LocalDate.parse("2024-01-05"), 95);
		
		Mark[] updatedMarksFromMethod = studentsService.addMark(existedStudent.id(), newMark).toArray(Mark[]::new);
		Mark[] updatedMarksFromDB = studentsService.getMarks(existedStudent.id()).toArray(Mark[]::new);
		
		
		Mark[] currentTestMarks = dbCreation.getStudentMarks(existedStudent.id());
		Mark[] updatedTestMarks = Arrays.copyOf(currentTestMarks, currentTestMarks.length + 1); 
		updatedTestMarks[updatedTestMarks.length - 1] = newMark;
		
		assertArrayEquals(updatedMarksFromMethod, updatedTestMarks);
		assertArrayEquals(updatedMarksFromDB, updatedTestMarks);
		
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.addMark(notExistedStudent.id(), newMark));
		
	}

	
	@Test
	@DisplayName("remove student test")
	void removeStudentTest() {
		
		Student removedStudent = studentsService.removeStudent(existedStudent.id());
		assertEquals(removedStudent, existedStudent);
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getStudent(existedStudent.id()));
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getStudent(notExistedId));
		
	}

	
	@Test
	void getStudentPhoneTest() {
		Student student2 = dbCreation.getStudent(2);
		assertEquals(student2, studentsService.getStudentByPhone(DbTestCreation.PONE_2));
		assertNull(studentsService.getStudentByPhone("kuku"));
	}
	
	@Test
	void getStudentsPhonePrefixTest() {
		List<Student> expected = List.of(dbCreation.getStudent(2));
		String phonePrefix = DbTestCreation.PONE_2.substring(0, 3);
		List<Student> actual = studentsService.getStudentsByPhonePrefix(phonePrefix);
		assertIterableEquals(expected, actual);
		assertTrue(studentsService.getStudentsByPhonePrefix("kuku").isEmpty());
	}
	@Test
	void getGoodStudentsTest() {
		List<Student> expected = List.of(dbCreation.getStudent(4), dbCreation.getStudent(6));
		List<Student> actual = studentsService.getStudentsAllGoodMarks(70);
		assertIterableEquals(expected, actual);
		assertTrue(studentsService.getStudentsAllGoodMarks(100).isEmpty());
	}
	@Test
	void getStudentsFewMarksTest() {
		List<Student> expected = List.of(dbCreation.getStudent(2), dbCreation.getStudent(7));
		List<Student> actual = studentsService.getStudentsFewMarks(2);
		assertIterableEquals(expected, actual);
		assertTrue(studentsService.getStudentsFewMarks(0).isEmpty());
	}
	@Test
	void getGoodStudentsSubjectTest() {
		//TODO
		List<Student> expected = List.of(dbCreation.getStudent(1), dbCreation.getStudent(3), dbCreation.getStudent(6));//.stream().sorted((s1, s2) -> Long.compare(s1.id(), s2.id())).toList();
		List<Student> actual = studentsService.getStudentsAllGoodMarksSubject(DbTestCreation.SUBJECT_1, 80).stream().sorted((s1, s2) -> Long.compare(s1.id(), s2.id())).toList();
		assertIterableEquals(expected, actual);
		
		List<Student> expected2 = List.of(dbCreation.getStudent(4), dbCreation.getStudent(6));//.stream().sorted((s1, s2) -> Long.compare(s1.id(), s2.id())).toList();
		List<Student> actual2 = studentsService.getStudentsAllGoodMarksSubject(DbTestCreation.SUBJECT_2, 80).stream().sorted((s1, s2) -> Long.compare(s1.id(), s2.id())).toList();
		assertIterableEquals(expected2, actual2);
		
		assertTrue(studentsService.getStudentsAllGoodMarksSubject(DbTestCreation.SUBJECT_5, 60).isEmpty());
	}
	@Test
	void getStudentsMarksAmountBetween() {
		//TODO
		List<Student> expected = List.of(dbCreation.getStudent(2), dbCreation.getStudent(7));
		List<Student> actual = studentsService.getStudentsMarksAmountBetween(0, 1);
		assertIterableEquals(expected, actual);
		
		List<Student> expected2 = List.of(dbCreation.getStudent(1), dbCreation.getStudent(4), dbCreation.getStudent(6));
		List<Student> actual2 = studentsService.getStudentsMarksAmountBetween(3, 10).stream().sorted((s1, s2) -> Long.compare(s1.id(), s2.id())).toList();;
		actual2.stream().forEach(e -> System.out.println(e.id()));
		assertIterableEquals(expected2, actual2);
		
		assertTrue(studentsService.getStudentsMarksAmountBetween(7, 20).isEmpty());
	}
	
}
