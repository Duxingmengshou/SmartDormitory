#ifndef QT_OPENCV_H
#define QT_OPENCV_H

#include <QMainWindow>
#include <QVBoxLayout>
#include <QMediaPlayer>
#include <QVideoWidget>
#include <QApplication>
#include <QMainWindow>
#include <QLabel>
#include <QTimer>
#include <QDateTime>
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QTcpServer>
#include <QTcpSocket>
#include <thread>
#include <chrono>
#include <QThread>
#include <QVector>


#include "./ui_SmartDormitoryQt.h"
#include "opencv_camera.hpp"

QT_BEGIN_NAMESPACE
namespace Ui { class qt_opencv; }
QT_END_NAMESPACE

class qt_opencv : public QMainWindow
{
    Q_OBJECT

public:
    qt_opencv(QWidget *parent = nullptr);
    ~qt_opencv();
    void getTcpLink();
    void foo();

private slots:
    void on_pushButtonClose_clicked();
    void on_pushButtonOpen_clicked();

    void on_pushButtonDList_clicked();

    void on_comboBoxSN_currentIndexChanged(int index);

private:
    Ui::qt_opencv *ui;
    opencv_camera *cap;
    QTimer timer;
    bool isShow;
    QDateTime dateTime;
    QNetworkAccessManager *manager;
    QNetworkRequest *request;
    QVector<QString> SNList;
};
#endif // QT_OPENCV_H
