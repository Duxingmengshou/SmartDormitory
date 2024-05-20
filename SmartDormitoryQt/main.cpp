#include <QApplication>
#include <QMainWindow>
#include <QLabel>
#include <QTimer>
#include <QDateTime>
#include <opencv2/opencv.hpp>
#include "opencv_camera.hpp"
#include "SmartDormitoryQt.h"
using namespace cv;

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    qt_opencv qv;
    qv.setWindowIcon(QIcon(":/img/img/监控.png"));
    qv.show();
    return app.exec();
}
