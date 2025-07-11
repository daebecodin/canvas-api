package edu.ksu.canvas.impl;

import com.google.gson.reflect.TypeToken;

import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.interfaces.EnrollmentReader;
import edu.ksu.canvas.interfaces.EnrollmentWriter;
import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetEnrollmentOptions;

import edu.ksu.canvas.requestOptions.UnEnrollOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnrollmentImpl extends BaseImpl<Enrollment, EnrollmentReader, EnrollmentWriter> implements EnrollmentReader,EnrollmentWriter {
    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentImpl.class);

    public EnrollmentImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient,
                          int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout,
                paginationPageSize, serializeNulls);
    }

    @Override
    public List<Enrollment> getUserEnrollments(GetEnrollmentOptions options) throws IOException {
        LOG.debug("Retrieving user enrollments for user {}", options.getObjectId());
        String url = buildCanvasUrl("users/" + options.getObjectId() + "/enrollments", options.getOptionsMap());
        return getListFromCanvas(url);
    }

    @Override
    public List<Enrollment> getSectionEnrollments(GetEnrollmentOptions options) throws IOException {
        LOG.debug("Retrieving section enrollments for section {}", options.getObjectId());
        String url = buildCanvasUrl("sections/" + options.getObjectId() + "/enrollments", options.getOptionsMap());
        return getListFromCanvas(url);
    }

    @Override
    public List<Enrollment> getCourseEnrollments(GetEnrollmentOptions options) throws IOException {
        LOG.debug("Retrieving course enrollments for course {}", options.getObjectId());
        String url = buildCanvasUrl("courses/" + options.getObjectId() + "/enrollments", options.getOptionsMap());
        return getListFromCanvas(url);
    }

    @Override
    @Deprecated
    public Optional<Enrollment> enrollUser(Enrollment enrollment) throws IOException, ProtocolException, URISyntaxException {
        if (enrollment.getCourseId() == null || enrollment.getCourseId() == 0) {
            throw new IllegalArgumentException("Required CourseId in enrollment was not found.");
        }
        return enrollUser(enrollment, false);
    }

    @Override
    public Optional<Enrollment> enrollUserInCourse(Enrollment enrollment) throws IOException, ProtocolException, URISyntaxException {
        if (enrollment.getCourseId() == null || enrollment.getCourseId() == 0) {
            throw new IllegalArgumentException("Required CourseId in enrollment was not found.");
        }
        LOG.debug("Enrolling user {} in course {}", enrollment.getUserId(), enrollment.getCourseId());
        return enrollUser(enrollment, false);
    }

    @Override
    public Optional<Enrollment> enrollUserInSection(Enrollment enrollment) throws IOException, ProtocolException, URISyntaxException {
        if (StringUtils.isBlank(enrollment.getCourseSectionId())) {
            throw new IllegalArgumentException("Required CourseSectionId in enrollment was not found.");
        }
        LOG.debug("Enrolling user {} in section {}", enrollment.getUserId(), enrollment.getCourseSectionId());
        return enrollUser(enrollment,true);
    }

    @Override
    public Optional<Enrollment> dropUser(String courseId, String enrollmentId) throws IOException, ProtocolException, URISyntaxException {
        return dropUser(courseId, enrollmentId, UnEnrollOptions.DELETE);
    }

    @Override
    public Optional<Enrollment> dropUser(String courseId, String enrollmentId, UnEnrollOptions unEnrollOption) throws IOException, ProtocolException, URISyntaxException {
        LOG.debug("Removing enrollment {} from course {}", enrollmentId, courseId);
        Map<String, List<String>> postParams = new HashMap<>();
        postParams.put("task", Collections.singletonList(unEnrollOption.toString()));
        String url = buildCanvasUrl("courses/" + courseId + "/enrollments/" + enrollmentId, Collections.emptyMap());
        Response response = canvasMessenger.deleteFromCanvas(oauthToken, url, postParams);
        if (response.getErrorHappened() ||  response.getResponseCode() != 200) {
            LOG.error("Failed to drop user from course, error message: {}", response);
            return Optional.empty();
        }
        return responseParser.parseToObject(Enrollment.class, response);
    }

    private Optional<Enrollment> enrollUser(Enrollment enrollment, boolean isSectionEnrollment) throws IOException, ProtocolException, URISyntaxException {
        String createdUrl = null;
        if (isSectionEnrollment) {
            createdUrl = buildCanvasUrl("sections/" + enrollment.getCourseSectionId() + "/enrollments", Collections.emptyMap());
        } else {
            createdUrl = buildCanvasUrl("courses/" + enrollment.getCourseId() + "/enrollments", Collections.emptyMap());
        }
        LOG.debug("create URl for course enrollments: {}", createdUrl);
        Response response = canvasMessenger.sendToCanvas(oauthToken, createdUrl, enrollment.toPostMap(serializeNulls));
        if (response.getErrorHappened() ||  response.getResponseCode() != 200) {
            LOG.error("Failed to enroll in course, error message: {}", response);
            return Optional.empty();
        }
        return responseParser.parseToObject(Enrollment.class,response);
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<Enrollment>>(){}.getType();
    }

    @Override
    protected Class<Enrollment> objectType() {
        return Enrollment.class;
    }

}
