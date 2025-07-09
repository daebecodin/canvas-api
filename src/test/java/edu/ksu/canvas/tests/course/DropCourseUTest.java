package edu.ksu.canvas.tests.course;

import edu.ksu.canvas.CanvasTestBase;
import edu.ksu.canvas.constants.CanvasConstants;
import edu.ksu.canvas.impl.EnrollmentImpl;
import edu.ksu.canvas.interfaces.EnrollmentWriter;
import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.net.FakeRestClient;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public class DropCourseUTest extends CanvasTestBase {
    @Autowired
    private FakeRestClient fakeRestClient;
    private EnrollmentWriter enrollmentsWriter;

    @Before
    public void setupData() {
        enrollmentsWriter = new EnrollmentImpl(baseUrl, apiVersion, SOME_OAUTH_TOKEN, fakeRestClient, SOME_CONNECT_TIMEOUT,
                SOME_READ_TIMEOUT, DEFAULT_PAGINATION_PAGE_SIZE, false);
    }

    @Test
    public void testEnrollUser() throws IOException, ProtocolException, URISyntaxException {
        //should make sure user exists
        String url = baseUrl + "/api/v1/courses/25/enrollments";
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentResponse.json");
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(25L);
        enrollment.setUserId("78839");
        Optional<Enrollment> enrollmentResponse = enrollmentsWriter.enrollUserInCourse(enrollment);
        Assert.assertEquals(25, (long) enrollmentResponse.get().getCourseId());
        Assert.assertEquals("78839", enrollmentResponse.get().getUserId());
        Assert.assertTrue(enrollmentResponse.get().getId()!=0);
    }

    @Test
    public void testDropEnrolledUser() throws IOException, ProtocolException, URISyntaxException {
        String url = baseUrl + "/api/v1/courses/25/enrollments/355047";
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentDeleteResponse.json");
        Optional<Enrollment> dropResponse = enrollmentsWriter.dropUser("25", "355047");
        Assert.assertEquals("deleted", dropResponse.get().getEnrollmentState());
    }

    @Test
    public void testSisUserMasqueradeEnrollUser() throws IOException, ProtocolException, URISyntaxException {
        //should make sure user exists
        String userId = "899123456";
        String url = baseUrl + "/api/v1/courses/25/enrollments?as_user_id=" + CanvasConstants.MASQUERADE_SIS_USER + ":" + userId;
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentResponse.json");
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(25L);
        enrollment.setUserId("78839");
        Optional<Enrollment> enrollmentResponse = enrollmentsWriter.writeAsSisUser(userId).enrollUser(enrollment);
        Assert.assertEquals(25, (long) enrollmentResponse.get().getCourseId());
        Assert.assertEquals("78839", enrollmentResponse.get().getUserId());
        Assert.assertTrue(enrollmentResponse.get().getId()!=0);
    }

    @Test
    public void testSisUserMasqueradeDropEnrolledUser() throws IOException, ProtocolException, URISyntaxException {
        String userId = "899123456";
        String url = baseUrl + "/api/v1/courses/25/enrollments/355047?as_user_id=" + CanvasConstants.MASQUERADE_SIS_USER + ":" + userId;
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentDeleteResponse.json");
        Optional<Enrollment> dropResponse = enrollmentsWriter.writeAsSisUser(userId).dropUser("25", "355047");
        Assert.assertEquals("deleted", dropResponse.get().getEnrollmentState());
    }

    @Test
    public void testCanvasUserMasqueradeEnrollUser() throws IOException, ProtocolException, URISyntaxException {
        //should make sure user exists
        String userId = "899123456";
        String url = baseUrl + "/api/v1/courses/25/enrollments?as_user_id=" + userId;
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentResponse.json");
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(25L);
        enrollment.setUserId("78839");
        Optional<Enrollment> enrollmentResponse = enrollmentsWriter.writeAsCanvasUser(userId).enrollUserInCourse(enrollment);
        Assert.assertEquals(25, (long) enrollmentResponse.get().getCourseId());
        Assert.assertEquals("78839", enrollmentResponse.get().getUserId());
        Assert.assertTrue(enrollmentResponse.get().getId()!=0);
    }

    @Test
    public void testCanvasUserMasqueradeDropEnrolledUser() throws IOException, ProtocolException, URISyntaxException {
        String userId = "899123456";
        String url = baseUrl + "/api/v1/courses/25/enrollments/355047?as_user_id=" + userId;
        fakeRestClient.addSuccessResponse(url, "SampleJson/EnrollmentDeleteResponse.json");
        Optional<Enrollment> dropResponse = enrollmentsWriter.writeAsCanvasUser(userId).dropUser("25", "355047");
        Assert.assertEquals("deleted", dropResponse.get().getEnrollmentState());
    }
}

