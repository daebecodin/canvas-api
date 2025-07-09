package edu.ksu.canvas.interfaces;

import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.requestOptions.UnEnrollOptions;
import org.apache.hc.core5.http.ProtocolException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public interface EnrollmentWriter extends CanvasWriter<Enrollment, EnrollmentWriter> {

     /**
      * Enroll a user in a course.
      *
      * @deprecated Use the <code>enrollUserInCourse</code> or <code>enrollUserInSection</code> methods instead.
      *
      * @param  enrollment partially populated Enrollment object
      * @return optional enrollment
      * @throws IllegalArgumentException when the enrollment courseID is not set
      * @throws IOException if there is an error communicating with Canvas
      */
     @Deprecated
     Optional<Enrollment> enrollUser(Enrollment enrollment) throws IllegalArgumentException, IOException, ProtocolException, URISyntaxException;

     /**
      * Enrolls a user in a course
      *
      * @param enrollment partially populated Enrollment object
      * @return optional enrollment
      * @throws IllegalArgumentException when the enrollment courseID is not set
      * @throws IOException if there is an error communicating with Canvas
      */
     Optional<Enrollment> enrollUserInCourse(Enrollment enrollment) throws IllegalArgumentException, IOException, ProtocolException, URISyntaxException;

     /**
      * Enrolls a user in a section.
      *
      * @param enrollment partially populated Enrollment object
      * @return optional enrollment
      * @throws IllegalArgumentException when the enrollment courseSectionId is not set
      * @throws IOException if there is an error communicating with Canvas
      */
     Optional<Enrollment> enrollUserInSection(Enrollment enrollment) throws IllegalArgumentException, IOException, ProtocolException, URISyntaxException;

     /**
      *
      * Drop a user enrolled in a course passing the UnEnrollOptions string in the task param.
      *
      * @param courseId - Canvas course identifier
      * @param enrollmentId - Canvas enrollment identifier
      * @param unEnrollOption - UnEnrollOption enum value of delete, conclude, inactivate or deactivate
      * @return Populated Enrollment Dropped when successful
      * @throws IOException if there is an error communicating with Canvas
      */
     Optional<Enrollment> dropUser(String courseId, String enrollmentId, UnEnrollOptions unEnrollOption) throws IOException, ProtocolException, URISyntaxException;

     /**
      *
      *  Drop a user enrolled in a course using the Unenrollment option of DELETE.
      *
      * @param courseId - Canvas course identifier
      * @param enrollmentId enrollmentId - Canvas enrollment identifier
      * @return Populated Enrollment Dropped when successful
      * @throws IOException if there is an error communicating with Canvas
      */
     Optional<Enrollment> dropUser(String courseId, String enrollmentId) throws IOException, ProtocolException, URISyntaxException;
}
