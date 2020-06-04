#include <DetectionBasedTracker_jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/detection_based_tracker.hpp>
#include <opencv2/core/core_c.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/legacy/legacy.hpp>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include "ImageUtils.h"
#include "detectObject.h"
#include "preprocessFace.h"
#include "recognition.h"

#include <iostream>
#include <fstream>
#include <vector>
#include <stdio.h>
#include <string>
#include <dirent.h>
#include <android/log.h>

const char *facerecAlgorithm = "FaceRecognizer.Fisherfaces";
const float UNKNOWN_PERSON_THRESHOLD = 0.7f;
const char *faceCascadeFilename = "lbpcascade_frontalface.xml";
const char *eyeCascadeFilename1 = "haarcascade_eye.xml";
const char *eyeCascadeFilename2 = "haarcascade_eye_tree_eyeglasses.xml";
const int faceWidth = 70;
const int faceHeight = faceWidth;
const int DESIRED_CAMERA_WIDTH = 640;
const int DESIRED_CAMERA_HEIGHT = 480;
const double CHANGE_IN_IMAGE_FOR_COLLECTION = 0.3;
const double CHANGE_IN_SECONDS_FOR_COLLECTION = 1.0;
const char *windowName = "Vision";
const int BORDER = 8;
const bool preprocessLeftAndRightSeparately = true;
bool loaded = false;
// Debug TAG for LogCat
#define LOG_TAG "FaceDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;

// Running modes.
enum MODES {
	MODE_STARTUP = 0,
	MODE_DETECTION,
	MODE_COLLECT_FACES,
	MODE_TRAINING,
	MODE_RECOGNITION,
	MODE_DELETE_ALL,
	MODE_END
};
const char* MODE_NAMES[] = { "Startup", "Detection", "Collect Faces", "Training", "Recognition", "Delete All", "ERROR!" };
MODES m_mode = MODE_STARTUP;

//int m_selectedPerson = -1;
int m_selectedPerson = 0;
int m_numPersons = 0;
vector<int> m_latestFaces;
// Position of GUI buttons:
Rect m_rcBtnAdd;
Rect m_rcBtnDel;
Rect m_rcBtnDebug;
int m_gui_faces_left = -1;
int m_gui_faces_top = -1;

CascadeClassifier faceCascade;
CascadeClassifier eyeCascade1;
CascadeClassifier eyeCascade2;

Ptr<FaceRecognizer> model;
vector<Mat> preprocessedFaces;
vector<int> faceLabels;
vector<string> nameLabels;
string personName;
Mat old_prepreprocessedFace;
double old_time = 0;

int img_counter = 0;
int personId_counter = 0;
bool hasCollect = false;
int lastLabel = -1;
/**
 * C++ conversion function between integers (or floats) to std::string.
 * @author Teo Xanthopoulos
 */
template<typename T> string toString(T t) {
	ostringstream out;
	out << t;
	return out.str();
}
/**
 * C++ conversion function between integers (or floats) to std::string.
 * @author Teo Xanthopoulos
 */
template<typename T> T fromString(string t) {
	T out;
	istringstream in(t);
	in >> out;
	return out;
}

extern "C" {
/**
 * Draw text into an image. Defaults to top-left-justified text,
 * but you can give negative x coords for right-justified text,
 * and/or negative y coords for bottom-justified text.
 * @author Teo Xanthopoulos
 * @return The bounding rect around the drawn text.
 */
Rect drawString(Mat img, string text, Point coord, Scalar color, float fontScale = 0.6f, int thickness = 1, int fontFace = FONT_HERSHEY_COMPLEX) {
	// Get the text size & baseline.
	int baseline = 0;
	Size textSize = getTextSize(text, fontFace, fontScale, thickness, &baseline);
	baseline += thickness;

	// Adjust the coords for left/right-justified or top/bottom-justified.
	if (coord.y >= 0) {
		// Coordinates are for the top-left corner of the text from the top-left of the image, so move down by one row.
		coord.y += textSize.height;
	} else {
		// Coordinates are for the bottom-left corner of the text from the bottom-left of the image, so come up from the bottom.
		coord.y += img.rows - baseline + 1;
	}
	// Become right-justified if desired.
	if (coord.x < 0) {
		coord.x += img.cols - textSize.width + 1;
	}

	// Get the bounding box around the text.
	Rect boundingRect = Rect(coord.x, coord.y - textSize.height, textSize.width,
			baseline + textSize.height);

	// Draw anti-aliased text.
	putText(img, text, coord, fontFace, fontScale, color, thickness, CV_AA);

	// Let the user know how big their text is, in case they want to arrange things.
	return boundingRect;
}
/**
 * Method for loading names from names.txt file
 * into vector<string> nameLabels
 * @author Teo Xanthopoulos
 * @return true if names loaded successfully, false if not
 */
bool loadNamesFromFile() {
	LOGD("load names from file");
	int count = 0;
	std::string line;
	std::ifstream namesFile("/data/data/com.teox.vision/names.txt");

	if(namesFile){
		LOGD("namesFile exists!");
		while (std::getline(namesFile, line)) {
				count++;
				nameLabels.push_back(line);
		}
		string s = toString(count);
		LOGD(s.c_str());
		return true;
	}else{
		LOGD("Error opening input file");
		return false;
	}
}
/**
 * Method for loading labels from labels.txt file
 * into vector<string> faceLabels
 * @author Teo Xanthopoulos
 * @return true if names loaded successfully, false if not
 */
bool loadLabelsFromFile() {
	LOGD("load labels from file");
	int count = 0;
	std::string line;
	std::ifstream labelsFile("/data/data/com.teox.vision/labels.txt");

	if(labelsFile){
		LOGD("labelsFile exists");
		while (std::getline(labelsFile, line)) {
				count++;
				faceLabels.push_back(atoi(line.c_str()));
		}
		string s = toString(count);
		LOGD(s.c_str());
		return true;
	}else{
		LOGD("Error opening input file");
		return false;
	}
}
/**
 * Method for loading images from image folder into
 * vector<Mat> preprocessedFaces
 * @author Teo Xanthopoulos
 * @return true if images loaded successfully, false if not
 */
bool loadFacesFromFile() {
	LOGD("load faces from file");
	DIR *pDir = NULL;
	struct dirent *pent = NULL;
	pDir = opendir("/data/data/com.teox.vision/images/");
	Mat img;

	if (pDir == NULL) {
		LOGD("pDir could't initialize directory!");
		return false;
	}

	while (pent = readdir(pDir)) {
		if (pent == NULL) {
			LOGD("ERROR! pent could't be initialized!");
			return false;
		}
		string s = pent->d_name;
		img = imread("/data/data/com.teox.vision/images/" + s, CV_LOAD_IMAGE_UNCHANGED);
		if (img.data) {
			preprocessedFaces.push_back(img);
		} else {
			LOGD("error loading image into vector");
		}
	}
	closedir(pDir);
	return true;
}
/**
 * Method for saving a label in the bottom
 * of labels.txt file
 * @author Teo Xanthopoulos
 */
void saveLabel2file(int label) {
	string clabel = toString(label);
	std::ofstream file("/data/data/com.teox.vision/labels.txt",	std::ios_base::app | std::ios_base::out);
	file << clabel + "\n";
	file.close();
}
/**
 * Method for saving a name in the bottom
 * of names.txt file
 * @author Teo Xanthopoulos
 */
void saveName2file(string name) {
	std::ofstream file("/data/data/com.teox.vision/names.txt", std::ios_base::app | std::ios_base::out);
	file << name + "\n";
	file.close();
}
/**
 * Method for saving images into image folder. Also it
 * forming the image names in the appropriate form
 * @author Teo Xanthopoulos
 * @throws cv::Exception
 */
void saveImg2disk(Mat img1, Mat img2, int personId) {

	int img2_counter = img_counter + 1;
	ostringstream convertIC, convertIC2, convertPI;
	convertIC << img_counter;
	convertIC2 << img2_counter;
	convertPI << personId;

	try {
		imwrite("/data/data/com.teox.vision/images/" + convertPI.str() + "_"+ convertIC.str() + ".png", img1);
		LOGD("img1 saved");
	} catch (cv::Exception &e) {
		LOGD("error saving image1 to disk");
	}

	try {
		imwrite("/data/data/com.teox.vision/images/" + convertPI.str() + "_"+ convertIC2.str() + ".png", img2);
		LOGD("img2 saved!");
	} catch (cv::Exception &e) {
		LOGD("error saving image2 to disk");
	}
	img_counter = img2_counter + 1;

}
/**
 * Method for getting the 1st file from image folder
 * @author Teo Xanthopoulos
 * @return Name of the 1st file as string, or ".." if image folder doesn't exists/there isn't any file in the image folder
 */
string getFirstFile() {
	string firstFile = "..";
	Mat img;
	DIR *pDir = NULL;
	struct dirent *pent = NULL;
	pDir = opendir("/data/data/com.teox.vision/images/");

	if (pDir == NULL) {
		LOGD("pDir could't initialize directory!");
		return "..";
	}

	while (pent = readdir(pDir)) {
		if (pent == NULL) {
			LOGD("Error! pent couldn't be initialized!");
			return "..";
			break;
		}
		firstFile = pent->d_name;
		img = imread("/data/data/com.teox.vision/images/" + firstFile, CV_LOAD_IMAGE_UNCHANGED);
		if (img.data) {
			firstFile = pent->d_name;
			break;
		}
	}
	closedir(pDir);
	img.release();
	return firstFile;
}
/**
 * Method for getting the last file from image folder.
 * @author Teo Xanthopoulos
 * @return Name of the last file as string, or ".." if image folder doesn't exists/there isn't any file in the image folder
 */
string getLastFile() {
	string lastFile = "..";
	Mat img;
	DIR *pDir = NULL;
	struct dirent *pent = NULL;
	pDir = opendir("/data/data/com.teox.vision/images/");

	if (pDir == NULL) {
		LOGD("pDir could't initialize directory!");
		return "..";
	}

	while (pent = readdir(pDir)) {
		if (pent == NULL) {
			LOGD("Error! pent couldn't be initialized!");
			return "..";
			break;
		}
		lastFile = pent->d_name;
		img = imread("/data/data/com.teox.vision/images/" + lastFile, CV_LOAD_IMAGE_UNCHANGED);
		if (img.data) {
			lastFile = pent->d_name;
		}
	}
	closedir(pDir);
	img.release();
	return lastFile;
}
/**
 * Method for getting only the label from image name.
 * e.g. if image name is 10_0.png, it gets 10
 * @author Teo Xanthopoulos
 * @return Number of the label as int, or -1 if image name is invalid
 */
int getLabelFromString(string s) {
	if (s == "..") {
		return -1;
	} else {
		char first = s[1];
		char second = s[2];
		char third = s[3];
		char fourth = s[4];
		char result[32];

		if(first == '_'){
			// 1_0
			result[0] = s[0];
			result[1] = 0;
			LOGD("1st case\n");
			LOGD("%d", atoi(result));
			return atoi(result);
		}else if(second == '_'){
			// 10_0
			result[0] = s[0];
			result[1] = s[1];
			result[2] = 0;
			LOGD("2nd case\n");
			LOGD("%d", atoi(result));
			return atoi(result);
		}else if(third == '_'){
			// 100_0
			result[0] = s[0];
			result[1] = s[1];
			result[2] = s[2];
			result[3] = 0;
			LOGD("3rd case\n");
			LOGD("%d", atoi(result));
			return atoi(result);
		}else if(fourth == '_'){
			// 1000_0
			result[0] = s[0];
			result[1] = s[1];
			result[2] = s[2];
			result[3] = s[3];
			result[4] = 0;
			LOGD("4th case\n");
			LOGD("%d", atoi(result));
			return atoi(result);
		}else{
			LOGD("unknown case..");
			return -1;
		}
	}
}
/**
 * Method that assigns a label to a name.
 * @author Teo Xanthopoulos
 * @param id Integer with the label
 * @return Corresponding name as string
 */
string id2name(int id) {
	String name;
	int pointer;
	for (int i = 0; i < faceLabels.size(); i++) {
		if (faceLabels[i] == id) {
			pointer = i;
			break;
		}
	}
	name = nameLabels[pointer];
	return name;
}
/**
 * Method for redefine some variables/vectors
 * for proper use of the program
 * @author Teo Xanthopoulos
 */
void redefineValues(){
	nameLabels.clear();
	faceLabels.clear();
	preprocessedFaces.clear();
	m_latestFaces.clear();

	personId_counter = 0;
	//m_selectedPerson = -1;
	m_selectedPerson = 0;
	m_numPersons = 0;
}
/**
 * JNI function for getting the number of trained persons from trainedModel.xml.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @throws cv::Exception
 * @return Number of trained persons as int, -1 if trainedModel couldn't load
 */
JNIEXPORT int JNICALL Java_com_teox_vision_VisionView_nativeGetNumOfTrainedFaces(){
	model = Algorithm::create<FaceRecognizer>(facerecAlgorithm);
	try {
		model->load("/data/data/com.teox.vision/trainedModel.xml");
	} catch (cv::Exception &e) {
		LOGD("Cannot Load Model!");
		return -1;
	}

	if(model){
		Mat mLabels = model->get<Mat>("labels");
		int MatCols = mLabels.cols;
		int number = mLabels.at<int>(0, MatCols-1) + 1;
		return number;
	}else{
		return -1;
	}
}
/**
 * JNI function for getting the number of new non-trained persons.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @return Number of non-trained persons as int, -1 if getLableFromString() function returned -1
 */
JNIEXPORT int JNICALL Java_com_teox_vision_VisionView_nativeGetNumOfNonTrainedFaces(){
	int lastLabel = getLabelFromString(getLastFile());
	if(lastLabel != -1){
		return lastLabel + 1;
	}else{
		return -1;
	}
}
/**
 * JNI function for redefine some variables/vector in this unit.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeRedefineValues()
{
	redefineValues();
}
/**
 * JNI function for debugging. It shows some variables into LogCat.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeShowValues() {
	/*LOGD("--lastLabel: %d", lastLabel);
	int n = getNumOfTrainedFaces();
	int z = getNumofNonTrainedFaces();
	LOGD("num of trained: %d", n);
	LOGD("num of non trained: %d", z);*/
}
/**
 * JNI function for checking the state of image collection. It initializes
 * some variables.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeCheckCollection() {
	string s = getLastFile();
	LOGD("last file: %s", s.c_str());
	if (s.compare("..") == 0) {
		hasCollect = false;
		lastLabel = -1;
		LOGD("hasCollect = false");
	} else {
		hasCollect = true;
		LOGD("hasCollect = true");
		lastLabel = getLabelFromString(s);
		m_selectedPerson = lastLabel + 1;
		LOGD("integer LAST label: %d", lastLabel);
		LOGD("integer NEXT label: %d", m_selectedPerson);
	}
}
/**
 * JNI function for starting the training process. It loads
 * names/labels/images into vectors and then trains the system
 * by calling learnCollectedFaces() function. Then it saves the
 * trainedModel into application folder.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @throws cv::Exception
 * @return true if model saved successfully, false if not
 */
JNIEXPORT bool JNICALL Java_com_teox_vision_VisionView_nativeBeginTrainning() {
	// Check if there is enough data to train from. For Eigenfaces, we can learn
	// just one person if we want, but for Fisherfaces,
	// we need at least 2 people otherwise it will crash!
	bool namesOk = loadNamesFromFile();
	bool labelsOk = loadLabelsFromFile();
	bool facesOk = loadFacesFromFile();

	int firstLabel = getLabelFromString(getFirstFile());
	int lastLabel = getLabelFromString(getLastFile());

	LOGD("first label: %d", firstLabel);
	LOGD("last label: %d", lastLabel);

	if((firstLabel != -1) && (lastLabel != -1)){
		if(!(lastLabel > firstLabel)){
			LOGD("need at least 2 people for training!");
			return false;
		}
	}else{
		LOGD("something went wrong with first and last label");
		return false;
	}

	//Check if names has loaded successfully from file to vector<string> nameLabels
	if(!namesOk){
		LOGD("fail to load names from file!");
		return false;
	}
	//Check if labels has loaded successfully from file to vector<int> faceLabels
	if(!labelsOk){
		LOGD("fail to load labels from file!");
		return false;
	}
	//Check if faces has loaded successfully from file to vector<Mat> preprocessedFaces
	if(!facesOk){
		LOGD("fail to load faces from file");
		return false;
	}

	if (preprocessedFaces.size() <= 0 || preprocessedFaces.size() != faceLabels.size()) {
		LOGD("Warning: Need some training data before it can be learnt! Collect more data ...");
		return false;
	}

	 // Start training from the collected faces using Eigenfaces or a similar algorithm.
	 model = learnCollectedFaces(preprocessedFaces, faceLabels, facerecAlgorithm);

	 //Save the trained model into the phone as xml file
	 try{
		 model->save("/data/data/com.teox.vision/trainedModel.xml");
		 LOGD("trained Model saved successfully!");
	 }catch(cv::Exception &e){
		 LOGD("fail to save trained Model");
		 return false;
	 }
	 //Empty vectors
	 nameLabels.clear();
	 faceLabels.clear();
	 preprocessedFaces.clear();

	return true;
}
/**
 * JNI function for loading names from file into vector.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @return true if names loaded successfully, false if not
 */
JNIEXPORT bool JNICALL Java_com_teox_vision_VisionView_nativeLoadNames() {
	bool isOk = false;
	try {
		isOk = loadNamesFromFile();
	} catch (cv::Exception &e) {
		isOk = false;
	}
	return isOk;
}
/**
 * JNI function for loading labels from file into vector.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @return true if labels loaded successfully, false if not
 */
JNIEXPORT bool JNICALL Java_com_teox_vision_VisionView_nativeLoadLabels() {
	bool isOk = false;
	try {
		isOk = loadLabelsFromFile();
	} catch (cv::Exception &e) {
		isOk = false;
	}
	return isOk;
}
/**
 * JNI function for loading trainedModel from file.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 * @return true if trainedModel loaded successfully, false if not
 */
JNIEXPORT bool JNICALL Java_com_teox_vision_VisionView_nativeLoadModel() {
	model = Algorithm::create<FaceRecognizer>(facerecAlgorithm);
	try {
		model->load("/data/data/com.teox.vision/trainedModel.xml");
		return true;
	} catch (cv::Exception &e) {
		LOGD("Cannot Load Model!");
		return false;
	}
}
/**
 * JNI function for toggling to Collect Faces Mode
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeToggleAdd() {
	img_counter = 0;
	m_mode = MODE_COLLECT_FACES;
}
/**
 * JNI function for toggling to Recognition Mode
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeToggleRecognize() {
	m_mode = MODE_RECOGNITION;
}
/**
 * JNI function for toggling to StartUp Mode
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeToggleStartUp() {
	m_mode = MODE_STARTUP;
}
/**
 * JNI function for getting person's name
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_nativeSendpName
(JNIEnv* env, jobject, jstring js)
{
	const char* jpName = env->GetStringUTFChars(js, NULL);
	personName = jpName;
	LOGD(personName.c_str());
}
/**
 * JNI function for initializing the three detectors (1 LBP, 2 HAAR).
 * It gets the paths from parameters, and then load them one
 * by one by calling FaceRecognizer::load() function.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_initDetectors
(JNIEnv* env, jobject, jstring jfaceCascade, jstring jeyeCascade, jstring jeyeGCascade)
{
	const char* jface = env->GetStringUTFChars(jfaceCascade, NULL);
	const char* jeye = env->GetStringUTFChars(jeyeCascade, NULL);
	const char* jeyeG = env->GetStringUTFChars(jeyeGCascade, NULL);

	try {
		faceCascade.load(jface);
	} catch (cv::Exception &e) {
		LOGD("failed to load faceCascade classifier!");
	}

	try {
		eyeCascade1.load(jeye);
	} catch (cv::Exception &e) {
		LOGD("failed to load eyeCascade classifier!");
	}

	try {
		eyeCascade2.load(jeyeG);
	} catch (cv::Exception &e) {
		LOGD("failed to load eyeGlassesCascade classifier!");
	}
}
/**
 * JNI function for processing the camera image. Called every time a camera
 * frame arrives. Initially it detects the face in the input image. Then
 * it works with modes, depending on the mode (m_mode) performs different
 * operations like Collecting faces or Recognizing the person in the input image.
 * Also, at the start it converts the input frame from Android's default pixel format BGRA (NV21, YUV420sp)
 * to OpenCV's default pixel format BGR, so OpenCV can process it. Then, at the end it converts it back to
 * Android's default pixel format.
 * Can be called from Java part.
 * @author Teo Xanthopoulos
 */
JNIEXPORT void JNICALL Java_com_teox_vision_VisionView_ShowPreview
(JNIEnv* env, jobject, jint width, jint height, jbyteArray yuv, jintArray bgra)
{
	// Get native access to the given Java arrays.
	jbyte* _yuv = env->GetByteArrayElements(yuv, 0);
	jint* _bgra = env->GetIntArrayElements(bgra, 0);

	// Prepare a cv::Mat that points to the YUV420sp data.
	Mat myuv(height + height / 2, width, CV_8UC1, (uchar *) _yuv);
	// Prepare a cv::Mat that points to the BGRA output data.
	Mat mbgra(height, width, CV_8UC4, (uchar *) _bgra);

	// Convert the color format from the camera's YUV420sp
	// semi-planar format to OpenCV's default BGR color image.
	Mat mbgr(height, width, CV_8UC3); // Alocate a new image buffer
	cvtColor(myuv, mbgr, CV_YUV420sp2BGR);

	// OpenCV can now access/modify the BGR image "mbgr" and should store the output as the BGR image "displayedFrame".
	Mat displayedFrame(mbgr.size(), CV_8UC3);
	//displayedFrame = mbgr;
	mbgr.copyTo(displayedFrame);

	// Run the face recognition system on the camera image. It will draw some things onto
	// the given image, so make sure it is not read-only memory!
	int identity = -1;
	// Find a face and preprocess it to have a standard size and contrast & brightness.
	Rect faceRect;  // Position of detected face.
	Rect searchedLeftEye, searchedRightEye; // top-left and top-right regions of the face, where eyes were searched.
	Point leftEye, rightEye;    // Position of the detected eyes.
	Mat preprocessedFace = getPreprocessedFace(displayedFrame, faceWidth, faceCascade, eyeCascade1, eyeCascade2,
											   preprocessLeftAndRightSeparately, &faceRect, &leftEye, &rightEye,
											   &searchedLeftEye, &searchedRightEye);

	bool gotFaceAndEyes = false;
	if (preprocessedFace.data)
		gotFaceAndEyes = true;

	// Draw an anti-aliased rectangle around the detected face.
	if (faceRect.width > 0) {
		rectangle(displayedFrame, faceRect, CV_RGB(255, 255, 0), 2, CV_AA);

		// Draw light-blue anti-aliased circles for the 2 eyes.
		Scalar eyeColor = CV_RGB(0,255,255);
		if (leftEye.x >= 0) {   // Check if the eye was detected
			circle(displayedFrame, Point(faceRect.x + leftEye.x, faceRect.y + leftEye.y), 6, eyeColor, 1, CV_AA);
		}
		if (rightEye.x >= 0) {   // Check if the eye was detected
			circle(displayedFrame,
					Point(faceRect.x + rightEye.x, faceRect.y + rightEye.y), 6,
					eyeColor, 1, CV_AA);
		}
	}

	if (m_mode == MODE_DETECTION) {
		//Dont do anything special
	} else if (m_mode == MODE_COLLECT_FACES) {
		if (hasCollect) {
			//we already have stored photos
			if (gotFaceAndEyes) {
				// Check if this face looks somewhat different from the previously collected face.
				double imageDiff = 10000000000.0;
				if (old_prepreprocessedFace.data) {
					imageDiff = getSimilarity(preprocessedFace,	old_prepreprocessedFace);
				}
				// Also record when it happened.
				double current_time = (double) getTickCount();
				double timeDiff_seconds = (current_time - old_time)	/ getTickFrequency();

				// Only process the face if it is noticeably different from the previous frame and there has been noticeable time gap.
				if ((imageDiff > CHANGE_IN_IMAGE_FOR_COLLECTION) && (timeDiff_seconds > CHANGE_IN_SECONDS_FOR_COLLECTION)) {
					// Also add the mirror image to the training set, so we have more training data, as well as to deal with faces looking to the left or right.
					Mat mirroredFace;
					flip(preprocessedFace, mirroredFace, 1);

					saveImg2disk(preprocessedFace, mirroredFace, m_selectedPerson);
					saveLabel2file(m_selectedPerson);
					saveLabel2file(m_selectedPerson);
					saveName2file(personName);
					saveName2file(personName);

					// Make a white flash on the face, so the user knows a photo has been taken.
					Mat displayedFaceRegion = displayedFrame(faceRect);
					displayedFaceRegion += CV_RGB(90,90,90);

					// Keep a copy of the processed face, to compare on next iteration.
					old_prepreprocessedFace = preprocessedFace;
					old_time = current_time;
				} // end of imageDiff if_statement
 			} // end of got Face&Eyes if_statement
		} else if (!hasCollect) {
			//we dont have any photo
			// Check if we have detected a face.
			if (gotFaceAndEyes) {
				// Check if this face looks somewhat different from the previously collected face.
				double imageDiff = 10000000000.0;
				if (old_prepreprocessedFace.data) {
					imageDiff = getSimilarity(preprocessedFace,	old_prepreprocessedFace);
				}
				// Also record when it happened.
				double current_time = (double) getTickCount();
				double timeDiff_seconds = (current_time - old_time)	/ getTickFrequency();

				// Only process the face if it is noticeably different from the previous frame and there has been noticeable time gap.
				if ((imageDiff > CHANGE_IN_IMAGE_FOR_COLLECTION) && (timeDiff_seconds > CHANGE_IN_SECONDS_FOR_COLLECTION)) {
					// Also add the mirror image to the training set, so we have more training data, as well as to deal with faces looking to the left or right.
					Mat mirroredFace;
					flip(preprocessedFace, mirroredFace, 1);

					// Add the face images to the list of detected faces.
					saveImg2disk(preprocessedFace, mirroredFace, m_selectedPerson);
					saveLabel2file(m_selectedPerson);
					saveLabel2file(m_selectedPerson);
					saveName2file(personName);
					saveName2file(personName);

					// Make a white flash on the face, so the user knows a photo has been taken.
					Mat displayedFaceRegion = displayedFrame(faceRect);
					displayedFaceRegion += CV_RGB(90,90,90);

					// Keep a copy of the processed face, to compare on next iteration.
					old_prepreprocessedFace = preprocessedFace;
					old_time = current_time;
				}// end of imageDiff if_statement
			}// end of got Face&Eyes if_statement
		}// end of !hasCollect if_statement
	} else if (m_mode == MODE_RECOGNITION) {
		// Generate a face approximation by back-projecting the eigenvectors & eigenvalues.
		if (gotFaceAndEyes) {
			Mat reconstructedFace;
			reconstructedFace = reconstructFace(model, preprocessedFace);

			// Verify whether the reconstructed face looks like the preprocessed face, otherwise it is probably an unknown person.
			double similarity = getSimilarity(preprocessedFace, reconstructedFace);

			String outputStr;
			if (similarity < UNKNOWN_PERSON_THRESHOLD) {
				// Identify who the person is in the preprocessed face image.
				identity = model->predict(preprocessedFace);
				// Assign a to the right name
				outputStr = id2name(identity);
			} else {
				// Since the confidence is low, assume it is an unknown person.
				outputStr = "Unknown";
			}
			//Log the recognized person
			LOGD(outputStr.c_str());
			//calculate confidence
			double confidenceRatio = 1.0 - min(max(similarity, 0.0), 1.0);
			//display the recognized person
			putText(displayedFrame, outputStr.c_str(), Point(faceRect.x, faceRect.y + faceRect.height + 18), cv::FONT_HERSHEY_PLAIN, 1.8, cv::Scalar(255, 255, 255), 2,	CV_AA);
			//display the confidence
			putText(displayedFrame, toString(confidenceRatio), Point(faceRect.x, faceRect.y + faceRect.height + 36), cv::FONT_HERSHEY_PLAIN, 1.6, cv::Scalar(0, 255, 0), 2,	CV_AA);
		}
	}
	//Show text in the camera
	Rect rcHelp;
	// Show the current mode.
	if (m_mode >= 0 && m_mode < MODE_END) {
		string modeStr = string(MODE_NAMES[m_mode]);
		drawString(displayedFrame, modeStr,	Point(BORDER, -BORDER - 2 - rcHelp.height), CV_RGB(0,0,0)); // Black shadow
		drawString(displayedFrame, modeStr,	Point(BORDER + 1, -BORDER - 1 - rcHelp.height),	CV_RGB(0,255,0)); // Green text
	}
	// Display how many photos tooked from that person
	if(m_mode == MODE_COLLECT_FACES){
		Rect rcPhotos;
		ostringstream conv;
		if(img_counter == 0){
			conv << 0;
		}else{
			conv << img_counter / 2;
		}

		string numPhotos = "# "+string(conv.str()+" photos for " + personName);
		drawString(displayedFrame, numPhotos,	Point(width - 320, -BORDER - 2 - rcPhotos.height), CV_RGB(0,0,0)); // Black shadow
		drawString(displayedFrame, numPhotos,	Point(width - 320 + 1, -BORDER - 1 - rcPhotos.height),	CV_RGB(0,255,0)); // Green text
	}
	// Convert the output from OpenCV's BGR to Android's BGRA format
	cvtColor(displayedFrame, mbgra, CV_BGR2BGRA);

	// Release the native lock we placed on the Java arrays.
	env->ReleaseIntArrayElements(bgra, _bgra, 0);
	env->ReleaseByteArrayElements(yuv, _yuv, 0);
}
}//end of extern "C" (global C/C++ functions that aren't part of a C++ Class)
