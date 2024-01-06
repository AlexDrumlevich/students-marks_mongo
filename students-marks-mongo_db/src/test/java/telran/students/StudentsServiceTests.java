package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.exceptions.NotFoundException;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.model.StudentDoc;
import telran.students.service.StudentsService;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class StudentsServiceTests {
	@Autowired
	StudentsService studentsService;
	@Autowired
	DbTestCreation dbCreation;
	
	final private long notExistedId = 999;
	private Student notExistedStudent;
	private Student existedStudent;
	
	@BeforeAll
	void setUpBeforeAll() {
		notExistedStudent = new Student(notExistedId, "Vasya", "0555555555");
		existedStudent = dbCreation.students[0];
	}
	
	@BeforeEach
	void setUp() {
		dbCreation.createDB();
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

	
}
