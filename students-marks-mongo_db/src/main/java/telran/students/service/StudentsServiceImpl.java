package telran.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.bson.Document;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.Filter;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.ScriptOperators.Accumulator;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.model.Filters;
import com.mongodb.internal.operation.AggregateOperation;

import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;
import telran.exceptions.NotFoundException;
import telran.students.dto.IdName;
import telran.students.dto.IdNamePhone;
import telran.students.dto.Mark;
import telran.students.dto.MarksOnly;
import telran.students.dto.NameAvgScore;
import telran.students.dto.Student;
import telran.students.model.StudentDoc;
import telran.students.repo.StudentRepo;
@Service
@Slf4j
@RequiredArgsConstructor
public class StudentsServiceImpl implements StudentsService {
final StudentRepo studentRepo;
final MongoTemplate mongoTemplate;


	@Override
	@Transactional
	public Student addStudent(Student student) {
		long id = student.id();
		if(studentRepo.existsById(id)) {
			throw new IllegalStateException(String.format("Student %d already exists", id));
		}
		studentRepo.save(StudentDoc.of(student));
		
		log.debug("saved {}", student);
		return student;
	}

	@Override
	@Transactional
	public Student updatePhone(long id, String phone) {
		StudentDoc studentDoc = getStudentDoc(id);
		String oldPhone = studentDoc.getPhone();
		studentDoc.setPhone(phone);
		studentRepo.save(studentDoc);
		log.debug("student {}, old phone number {}, new phone number {}", id, oldPhone, phone);
		return studentDoc.build();
	}

	
	private StudentDoc getStudentDoc(long id) {
		return studentRepo.findById(id)
				.orElseThrow(() -> new NotFoundException(String.format("Student %d not found", id)));
	}

	
	@Override 
	public Student getStudent(long id) {
		return getStudentDoc(id).build();
	}
	
	@Override
	@Transactional
	public List<Mark> addMark(long id, Mark mark) {
		StudentDoc studentDoc = getStudentDoc(id);
		studentDoc.addMark(mark);
		studentRepo.save(studentDoc);
		log.debug("student {}, added mark {}", id, mark);
		return studentDoc.getMarks();
	}

	@Override
	@Transactional
	public Student removeStudent(long id) {
		StudentDoc studentDoc = studentRepo.findStudentNoMarks(id);
		if(studentDoc == null) {
			throw new NotFoundException(String.format("student %d not found",id));
		}
		studentRepo.deleteById(id);
		log.debug("removed student {}, marks {} ", id, studentDoc.getMarks());
		return studentDoc.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Mark> getMarks(long id) {
		StudentDoc studentDoc = studentRepo.findStudentMarks(id);
		if(studentDoc == null) {
			throw new NotFoundException(String.format("student %d not found",id));
		}
		log.debug("id {}, name {}, phone {}, marks {}",
				studentDoc.getId(), studentDoc.getName(), studentDoc.getPhone(), studentDoc.getMarks());
		return studentDoc.getMarks();
	}
	
	@Override
	public Student getStudentByPhone(String phoneNumber) {
		IdName studentDoc = studentRepo.findByPhone(phoneNumber);
		Student res = null;
		if (studentDoc != null) {
			res = new Student(studentDoc.getId(), studentDoc.getName(), phoneNumber);
		}
		return res;
	}

	@Override
	public List<Student> getStudentsByPhonePrefix(String phonePrefix) {
		List <IdNamePhone> students = studentRepo.findByPhoneRegex(phonePrefix + ".+");
		log.debug("number of the students having phone prefix {} is {}", phonePrefix, students.size());
		return getStudents(students);
	}

	private List<Student> getStudents(List<IdNamePhone> students) {
		return students.stream().map(inp -> new Student(inp.getId(), inp.getName(),
				inp.getPhone())).toList();
	}

	@Override
	public List<Student> getStudentsAllGoodMarks(int thresholdScore) {
		List<IdNamePhone> students = studentRepo.findByGoodMarks(thresholdScore);
		return getStudents(students);
	}

	@Override
	public List<Student> getStudentsFewMarks(int thresholdMarks) {
		List<IdNamePhone> students = studentRepo.findByFewMarks(thresholdMarks);
		return getStudents(students);
	}

	@Override
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		// TODO 
		//getting students who have at least one score of a given subject and all scores of that subject
		//greater than or equal a given threshold
		List<StudentDoc> studentsDoc = studentRepo.getStudentsAllGoodMarksSubject(subject, thresholdScore);
		return studentsDoc.stream().map(s ->  s.build()).toList();
	}

	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		// TODO 
		//getting students having number of marks in a closed range of the given values
		//nMarks >= min && nMarks <= max
		List<StudentDoc> studentsDoc = studentRepo.getStudentsMarksAmountBetween(min, max);
		return studentsDoc.stream().map(s -> s.build()).toList();
	}
	
	@Override
	public List<Mark> getStudentSubjectMarks(long id, String subject) {
		if (!studentRepo.existsById(id)) {
			throw new NotFoundException(String.format("student with id %d not found", id));
		}
		MatchOperation matchStudent = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchMarksSubject = Aggregation.match(Criteria.where("marks.subject").is(subject));
		ProjectionOperation projectionOperation = Aggregation.project("marks.score", "marks.date");
		Aggregation pipeLine = Aggregation.newAggregation(matchStudent, unwindOperation,
				matchMarksSubject, projectionOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class);
		List<Document> listDocuments = aggregationResult.getMappedResults();
		log.debug("listDocuments: {}", listDocuments);
		List<Mark> result = listDocuments.stream()
				.map(d -> new Mark(subject, d.getDate("date").toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate(), d.getInteger("score"))).toList();
				;
		log.debug("result: {}", result);
		return result;		
	}

	@Override
	public List<NameAvgScore> getStudentAvgScoreGreater(int avgScoreThreshold) {
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		GroupOperation groupOperation = Aggregation.group("name").avg("marks.score").as("avgMark");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("avgMark").gt(avgScoreThreshold));
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "avgMark");
		Aggregation pipeLine = Aggregation.newAggregation(unwindOperation, groupOperation, matchOperation, sortOperation);
		List<NameAvgScore> res = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class)
				.getMappedResults().stream().map(d -> new NameAvgScore(d.getString("_id"),
						d.getDouble("avgMark").intValue())).toList();
		log.debug("result: {}", res);
		return res;
	}
	
	
	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {
		// TODO 
		//returns list of Mark objects of the required student at the given dates
		//Filtering and projection should be done at DB server
		if (!studentRepo.existsById(id)) {
			throw new NotFoundException(String.format("student with id %d not found", id));
		}
		MatchOperation matchStudentById = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindStudentMarks = Aggregation.unwind("marks");
		ProjectionOperation projectionOperation = Aggregation.project("marks.subject", "marks.score", "marks.date");
		MatchOperation matchMarksByDateFrom = Aggregation.match(Criteria.where("date").gte(from));
		MatchOperation matchMarksByDateTo = Aggregation.match(Criteria.where("date").lte(to));
		SortOperation sortByMarkDate = Aggregation.sort(Direction.DESC, "date");
		
		Aggregation pipeLine = Aggregation.newAggregation(
				matchStudentById,
				unwindStudentMarks,
				projectionOperation,
				matchMarksByDateFrom,
				matchMarksByDateTo,
				sortByMarkDate
			); 
		
		mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class)
		.getMappedResults().stream().forEach(e -> {
			System.out.println(e.toJson());
			
		});
		System.out.println("Marks from repository method: " + getMarks(id).toString());
		
		List<Mark> marks = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class)
				.getMappedResults()
				.stream() 
				.map(m -> {
					
					//get to object inside another object ("marks.subject" does not work)
						/*
						Document marksDocument = (Document) m.get("marks");
						return new Mark(marksDocument.getString("subject"),
						marksDocument.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
						marksDocument.getInteger("score"));	
						*/
					
					return new Mark(
							m.getString("subject"),
							m.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
							m.getInteger("score")
						);
					
				})
				.toList();
			log.debug("listDocuments: {}", marks);	
			return marks;
	
	}

	@Override
	public List<Student> getBestStudents(int nStudents) {
		// TODO 
		//returns list of a given number of the best students
		//Best students are the ones who have most scores greater than 80
		
		
		//match field of array element:  match(Criteria.where("marks").elemMatch(Criteria.where("score").gt(80))
		//match field of object: .match(Criteria.where("marks.score").gt(80));
		MatchOperation matchByPresentScore80 = Aggregation.match(Criteria.where("marks").elemMatch(Criteria.where("score").gt(80)));
		UnwindOperation unwindMarks = Aggregation.unwind("marks");
		MatchOperation matchByScore80Only = Aggregation.match(Criteria.where("marks.score").gt(80));
		
		//the group creates a projection based on the passed fields. 
		//If there is more than one field, an internal object will be created under new id field
		GroupOperation groupByStudentId = Aggregation.group("id", "name", "phone").count().as("marksCount");
		SortOperation sortByMarksCount = Aggregation.sort( Direction.DESC,"marksCount", "name");
		LimitOperation limit = Aggregation.limit(nStudents);
		
		AggregationResults<Document> aggregationResults = mongoTemplate
			.aggregate(
					Aggregation.newAggregation(
							matchByPresentScore80,
							unwindMarks,
							matchByScore80Only,
							groupByStudentId,
							sortByMarksCount,
							limit
							),
					StudentDoc.class,
					Document.class
					);
		System.out.println("--------------------------");
		aggregationResults.getMappedResults().stream().forEach(r -> System.out.println(r.toJson()));
		
			List<Student> students = aggregationResults.getMappedResults()
			.stream()
			.map(s -> {
				Document student = (Document) s.get("_id");
				return new Student(
					student.getLong("id"),
					student.getString("name"),
					student.getString("phone")
					);	
				}
			)
			.toList();
			
		log.debug("students: {}", students);	
		return students;
	}

	@Override
	public List<Student> getWorstStudents(int nStudents) {
		// TODO 
		//returns list of a given number of the worst students
		//Worst students are the ones who have least sum's of all scores
		//Students who have no scores at all should be considered as worst
		//instead of GroupOperation to apply AggregationExpression (with AccumulatorOperators.Sum) and ProjectionOperation for adding new fields with computed values 
		
		UnwindOperation unwindByMarks = Aggregation.unwind("marks", true);
		GroupOperation groupByScoresSum = Aggregation.group("id", "name", "phone").sum("marks.score").as("sum");
		SortOperation sortByScoresSumAsc = Aggregation.sort(Direction.ASC, "sum");
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectionOperation = Aggregation.project("_id");
		
		Aggregation aggregation = Aggregation.newAggregation(
				unwindByMarks,
				groupByScoresSum,
				sortByScoresSumAsc,
				limitOperation,
				projectionOperation
				);
		
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, StudentDoc.class, Document.class); 
		aggregationResults.getMappedResults().stream().forEach(r -> System.out.println(r.toJson()));
		
		List<Student> students = aggregationResults
				.getMappedResults()
				.stream()
				.map(s -> {
					Document student = (Document) s.get("_id");
					return new Student(
						student.getLong("id"),
						student.getString("name"),
						student.getString("phone")
						);	
					}
				)
				.toList();
		
		return students;
	}



}
