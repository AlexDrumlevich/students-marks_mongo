package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.IntegerRange;
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
	private Student firstExistedStudent;
		
	@BeforeEach
	void setUp() {
		dbCreation.createDB();
		
		notExistedStudent = new Student(notExistedId, "Vasya", "0555555555");
		firstExistedStudent = dbCreation.students[0];
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
		
		
		assertThrowsExactly(IllegalStateException.class, () -> studentsService.addStudent(firstExistedStudent));

	}

	@Test
	@DisplayName("get student by id test")
	void getStudentTest() {
		assertEquals(firstExistedStudent, studentsService.getStudent(firstExistedStudent.id()));
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getStudent(notExistedId));

	}

	@Test
	@DisplayName("update phone test")
	void updatePhoneTest() {
	
		String newPhoneNumber = "999999999";
		assertNotEquals(firstExistedStudent.phone(), newPhoneNumber);
		
		Student updatedStudentFromMethod = studentsService.updatePhone(firstExistedStudent.id(), newPhoneNumber);
		Student updatedStudentFromDB = studentsService.getStudent(firstExistedStudent.id());
		
		StudentDoc existedStudentDoc = StudentDoc.of(firstExistedStudent);
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
		
		Mark[] updatedMarksFromMethod = studentsService.addMark(firstExistedStudent.id(), newMark).toArray(Mark[]::new);
		Mark[] updatedMarksFromDB = studentsService.getMarks(firstExistedStudent.id()).toArray(Mark[]::new);
		
		
		Mark[] currentTestMarks = dbCreation.getStudentMarks(firstExistedStudent.id());
		Mark[] updatedTestMarks = Arrays.copyOf(currentTestMarks, currentTestMarks.length + 1); 
		updatedTestMarks[updatedTestMarks.length - 1] = newMark;
		
		assertArrayEquals(updatedMarksFromMethod, updatedTestMarks);
		assertArrayEquals(updatedMarksFromDB, updatedTestMarks);
		
		
		assertThrowsExactly(NotFoundException.class, () -> studentsService.addMark(notExistedStudent.id(), newMark));
		
	}

	
	@Test
	@DisplayName("remove student test")
	void removeStudentTest() {
		
		Student removedStudent = studentsService.removeStudent(firstExistedStudent.id());
		assertEquals(removedStudent, firstExistedStudent);
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getStudent(firstExistedStudent.id()));
		
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
	
	
	@Test
	void getStudentSubjectMarks() {
		List<Mark> expected = List.of(new Mark(DbTestCreation.SUBJECT_1, DbTestCreation.DATE_1, 80 ),
				new Mark(DbTestCreation.SUBJECT_1, DbTestCreation.DATE_2, 90 ));
		List<Mark> actual = studentsService.getStudentSubjectMarks(1, DbTestCreation.SUBJECT_1);
		assertTrue(studentsService.getStudentSubjectMarks(4, DbTestCreation.SUBJECT_1).isEmpty());
		assertThrowsExactly(NotFoundException.class,
				() -> studentsService.getStudentSubjectMarks(1000, DbTestCreation.SUBJECT_1));
		assertIterableEquals(expected, actual);
		
	}
	
	
	@Test
	void getStudentMarksAtDatesTest() {
		//TODO
		/* 
		Id:6
		final static LocalDate DATE_1 = LocalDate.parse("2023-10-20");
		final static LocalDate DATE_2 = LocalDate.parse("2023-11-20");
		final static LocalDate DATE_3 = LocalDate.parse("2023-12-20");
		final static LocalDate DATE_4 = LocalDate.parse("2024-01-01");
		 
		 */
		List<Mark> expexted0 = dbCreation.getCertainMarksForStudent(DbTestCreation.ID_6, 4, 3, 2, 1);
		List<Mark> actual0 = studentsService.getStudentMarksAtDates(DbTestCreation.ID_6, LocalDate.parse("2023-01-20"), LocalDate.parse("2024-12-31"));
		assertIterableEquals(expexted0, actual0);
		
		List<Mark> expexted1 = dbCreation.getCertainMarksForStudent(DbTestCreation.ID_6, 3, 2);
		List<Mark> actual1 = studentsService.getStudentMarksAtDates(DbTestCreation.ID_6, LocalDate.parse("2023-11-20"), LocalDate.parse("2023-12-31"));
		assertIterableEquals(expexted1, actual1);
		
		List<Mark> expexted2 = dbCreation.getCertainMarksForStudent(DbTestCreation.ID_6, 4);
		List<Mark> actual2 = studentsService.getStudentMarksAtDates(DbTestCreation.ID_6, LocalDate.parse("2024-01-01"), LocalDate.parse("2024-10-10"));
		assertIterableEquals(expexted2, actual2);
		
		assertTrue(studentsService.getStudentMarksAtDates(DbTestCreation.ID_6, LocalDate.parse("2024-01-02"), LocalDate.parse("2024-10-10")).isEmpty());
		assertTrue(studentsService.getStudentMarksAtDates(DbTestCreation.ID_6, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-09-10")).isEmpty());
	}
	@Test
	void getBestStudentsTest() {
		List<Student> allBestStudents = List.of(dbCreation.getStudent(6), dbCreation.getStudent(4), dbCreation.getStudent(1));
		List<Student> best2Students = List.of(dbCreation.getStudent(6), dbCreation.getStudent(4));
		List<Student> theBestStudent = List.of(dbCreation.getStudent(6));

		
		assertIterableEquals(allBestStudents, studentsService.getBestStudents(10));
		assertIterableEquals(allBestStudents, studentsService.getBestStudents(3));
		assertIterableEquals(best2Students, studentsService.getBestStudents(2));
		assertIterableEquals(theBestStudent, studentsService.getBestStudents(1));
	}
	
	@Test
	void getWorstStudentsTest() {
		
		List<Student> studentsActuaListTheWorstBegining = new ArrayList<>();
		Mark[][] marks = dbCreation.marks;
		Student[] students = dbCreation.students;
		IntStream.range(0, marks.length)
			.boxed()
			.sorted((i1, i2) -> {
				int result = 0;
				if(marks[i1].length == 0 && marks[i2].length != 0) {
					result = -1;
				} else if (marks[i1].length != 0 && marks[i2].length == 0) {
					result = 1;
				} else if ((marks[i1].length != 0 && marks[i2].length != 0)) {
					result = Integer.compare(
							Arrays.stream(marks[i1]).map(m -> m.score()).collect(Collectors.summingInt(Integer::intValue)),
							Arrays.stream(marks[i2]).map(m -> m.score()).collect(Collectors.summingInt(Integer::intValue))
							);
				}
				return result;
			}).forEach(i -> studentsActuaListTheWorstBegining.add(students[i]));
		
		
		IntStream.rangeClosed(1, dbCreation.students.length).forEach(i -> {
			assertIterableEquals(studentsActuaListTheWorstBegining.subList(0, i), studentsService.getWorstStudents(i));
		});
	}
}
