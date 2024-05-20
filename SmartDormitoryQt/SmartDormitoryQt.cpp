#include "SmartDormitoryQt.h"
#include "./ui_SmartDormitoryQt.h"

qt_opencv::qt_opencv(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::qt_opencv)
{
    ui->setupUi(this);

    manager = new QNetworkAccessManager(this);
    isShow=false;
}


qt_opencv::~qt_opencv()
{
    delete ui;
}


void qt_opencv::on_pushButtonClose_clicked()
{
    if(isShow==false)
        return;
    timer.stop();
    delete cap;
    delete request;
    ui->labelVideo->setText("       设备未打开");
    isShow=false;
}


void qt_opencv::on_pushButtonOpen_clicked()
{
    if(isShow==true)
        return;
    //http://127.0.0.1:8089/4E67E4F83443
    QString textURL="http://"+
            ui->lineEditIP->text()+
            ":8089/"+
            ui->lineEditSN->text();
    request = new QNetworkRequest(QUrl(textURL));
    qDebug()<<textURL;


    //    http://127.0.0.1:8003/video/4E67E4F83443
    QString videoURL="http://"+
            ui->lineEditIP->text()+
            ":8003/video/"+
            ui->lineEditSN->text();
    cap=new opencv_camera(videoURL);
    if(!cap->capOK)
    {
        qDebug()<<"网络视频流打开失败，请检查设备SN";
        return;
    }
    qDebug()<<videoURL;



    // 创建定时器，定时刷新图像、文本框
    connect(&timer, &QTimer::timeout, [&]() {
        // 处理视频流信息
        Mat frame;
        cap->getFrame(frame);
        cv::resize(frame, frame, Size(640, 480));
        // 将 OpenCV Mat 转换为 Qt QImage
        QImage qImage(frame.data, frame.cols, frame.rows, frame.step, QImage::Format_RGB888);
        qImage = qImage.rgbSwapped(); // 颜色通道顺序调整为 RGB
        // 在标签上显示图像
        ui->labelVideo->setPixmap(QPixmap::fromImage(qImage).scaled(ui->labelVideo->size(), Qt::KeepAspectRatio));



        // 发起 GET 请求
        QNetworkReply *reply = manager->get(*request);
        // 处理响应
        connect(reply, &QNetworkReply::finished, [=]() {
            if (reply->error() == QNetworkReply::NoError) {
                // 获取响应的数据
                QByteArray responseData = reply->readAll();
                // 将数据显示在文本框中
                QString buf="";
                dateTime= QDateTime::currentDateTime();
                QString time = dateTime.toString("yyyy-MM-dd hh:mm:ss");
                buf+="当前设备时间：\n"+time+"\n";
                buf+=QString(responseData);
                qint64 currentTime = QDateTime::currentMSecsSinceEpoch();
                double elapsedTime = (currentTime - cap->lastTime) / 1000.0; // 转换为秒
                double fps = cap->frameCounter / elapsedTime;
                buf+=QString("当前视频帧率：\n")+QString::number(fps)+QString("fps\n");
                ui->labelMsg->setText(buf);

            } else {
                // 处理错误
                qDebug() << "Error: " << reply->errorString();
            }
            // 释放 reply 对象
            reply->deleteLater();
        });
    });
    timer.start(10);
    isShow=true;
}


void qt_opencv::on_pushButtonDList_clicked()
{
    QString baseURL="http://"+
            ui->lineEditIP->text()+
            ":8089/all";
    request = new QNetworkRequest(QUrl(baseURL));
    qDebug()<<baseURL;

    // 发起 GET 请求
    QNetworkReply *reply = manager->get(*request);
    // 处理响应
    connect(reply, &QNetworkReply::finished, [=]() {
        if (reply->error() == QNetworkReply::NoError) {
            // 获取响应的数据
            QByteArray responseData = reply->readAll();
            // 将数据显示在文本框中
            QString buf=QString(responseData);
            // 根据分隔符-分割字符串
            QStringList splitList = buf.split("-");
            // 将 QStringList 转换为 QVector<QString>
            SNList = splitList.toVector();
            ui->comboBoxSN->clear();
            for(auto c:SNList)
            {
                qDebug()<<c;
                if(c!="")
                    ui->comboBoxSN->addItem(c);
            }
        } else {
            // 处理错误
            qDebug() << "Error: " << reply->errorString();
        }
        // 释放 reply 对象
        reply->deleteLater();
    });
    delete request;
}


void qt_opencv::on_comboBoxSN_currentIndexChanged(int index)
{
    ui->lineEditSN->setText(ui->comboBoxSN->currentText());
}

