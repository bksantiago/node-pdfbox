'use strict';

const PageImage = require('./pdf-page-image');

class PDFPage {

  constructor(PDFPageJavaInstance) {
    this.PDFPageJavaInstance = PDFPageJavaInstance;
  }

  getPage() {
    return this.PDFPageJavaInstance;
  }

  getRectSync() {
    const rect = this.PDFPageJavaInstance.getRectSync();
    return {width: rect.getWidthSync(), height: rect.getHeightSync()};
  }

  getTextSync() {
    return this.PDFPageJavaInstance.getTextSync();
  }

  getImageSync() {
    return new PageImage(this.PDFPageJavaInstance.getImageSync());
  }

  getImageScaleSync(scale) {
    return new PageImage(this.PDFPageJavaInstance.getImageSync(scale));
  }

  getImageWithSizeSync(width, height) {
    return new PageImage(this.PDFPageJavaInstance.getImageSync(width, height));
  }

  getAspectFillScaleSync(width, height) {
    return this.PDFPageJavaInstance.getAspectFillScaleSync(width, height);
  }

  getAspectFitScaleSync(width, height) {
    return this.PDFPageJavaInstance.getAspectFitScaleSync(width, height);
  }

  extractPageSync(path) {
    this.PDFPageJavaInstance.extractPageSync(path);
  }

  writeImageSync(path) {
    this.PDFPageJavaInstance.writeImageSync(path, PDFPage.BOX_CROP, 768, 1024);
  }

}

module.exports = PDFPage;