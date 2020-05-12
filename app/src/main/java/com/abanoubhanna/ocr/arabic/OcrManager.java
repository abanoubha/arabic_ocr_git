package com.abanoubhanna.ocr.arabic;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

class OcrManager {
    private TessBaseAPI baseAPI = null;
    void initAPI() {
        baseAPI = new TessBaseAPI();
        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);

        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        //baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY); // what i used prev

        // after copy, my path to trainned data is getExternalFilesDir(null)+"/tessdata/"+"ara.traineddata";
        // but init() function just need parent folder path of "tessdata", so it is getExternalFilesDir(null)
        String dataPath = MainApplication.instance.getTessDataParentDirectory();
        baseAPI.init(dataPath,"ara", TessBaseAPI.OEM_LSTM_ONLY);
        // language code is name of trained data file, except extension part
        // "ara.traineddata" => language code is "ara"

        // first param is datapath which is  part to the your trainned data, second is language code
        // now, your trainned data stored in assets folder, we need to copy it to another external storage folder.
        // It is better do this work when application start firt time
    }

    String startRecognize(Bitmap bitmap) {
        if(baseAPI ==null)
            initAPI();
        baseAPI.setImage(bitmap);
        String OCRedText;
        try {
            OCRedText = baseAPI.getUTF8Text().trim(); //trim extracted text
        }catch (Exception e){
            return "ERROR 120";
        }
        baseAPI.end();
        return OCRedText;
    }
}
