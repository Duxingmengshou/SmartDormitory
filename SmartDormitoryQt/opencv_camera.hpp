#pragma once

#include <opencv2/opencv.hpp>
#include <QString>
#include <QDateTime>
using namespace cv;

class opencv_camera{
public:
    VideoCapture cap;
    qint64 lastTime = QDateTime::currentMSecsSinceEpoch();
    int frameCounter = 0;
    bool capOK=false;
    opencv_camera(){}
    opencv_camera(const QString &url){
        cap.open(url.toStdString());
        // 检查视频是否成功打开
        if (!cap.isOpened()) {
            qDebug() << "Error opening video stream or file";
            capOK=false;
        }
        else
        {
            capOK=true;
        }
    }
    void setUrl(const QString &url){
        cap.open(url.toStdString());
        // 检查视频是否成功打开
        if (!cap.isOpened()) {
            qDebug() << "Error opening video stream or file";
            capOK=false;
        }
        else
        {
            capOK=true;
        }
    }
    void getFrame(Mat& frame){
        frameCounter++;
        cap>>frame;
        // 检查帧是否为空
        if (frame.empty()) {
            qDebug() << "Empty frame";
        }
        else{
//            qint64 currentTime = QDateTime::currentMSecsSinceEpoch();
//            double elapsedTime = (currentTime - lastTime) / 1000.0; // 转换为秒
//            double fps = frameCounter / elapsedTime;
//            qDebug()<<fps;
        }
    }
    ~opencv_camera(){
        cap.release();
    }

};
