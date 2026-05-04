// Course navigation helper for dashboard
// Handles navigation from dashboard to course detail page

function navigateToCourse(courseId, shortName, fullName) {
    if (!courseId || !shortName) {
        console.error('Invalid course data');
        return;
    }

    // Store course info in localStorage for course.html to retrieve
    const courseData = {
        courseId: courseId,
        shortName: shortName,
        fullName: fullName || shortName,
        timestamp: Date.now()
    };

    localStorage.setItem('selectedCourse', JSON.stringify(courseData));

    // Navigate to course page
    window.location.href = `/course.html?id=${courseId}&name=${encodeURIComponent(shortName)}`;
}