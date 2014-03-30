package de.saviodimatteo.madnetsim.drawing;

import processing.core.*;
class DropDownMenu {
private static final long serialVersionUID = 1L;
  String[] data;
  private int KTextPaddingH = 20;
  private int KTextPaddingV = 5;
  private int KFontSize = 14;
  private int KBarWidth = 20;
  private int KButtonHeight = KFontSize + KTextPaddingH;
  private PFont genericFont;
  private PFont titleFont;
  
  private int n = 20;
  private int top = 0;
  private int bottom = n;
  private int x=0;
  private int y=0;
  
  private int pixHeight;
  private int pixWidth;
  
  private boolean opened;
  private String title;
  private int dataCount;
  PApplet app;
  
  DropDownMenu(PApplet aApplet, String[] lines, String menuTitle, int x, int y) {
	app = aApplet;
	this.x = x;
	this.y = y;
    data = lines;
    dataCount = data.length;
    if (dataCount < n)
    	n = dataCount;
    opened = false;
    title = menuTitle;
    
    int minLength = PApplet.MAX_INT;
    int maxLength = PApplet.MIN_INT;
    for (int ii=0; ii < dataCount; ii++) {
      String d = data[ii];
      if (minLength > d.length()) minLength = d.length();
      if (maxLength < d.length()) maxLength = d.length();
    }
    pixHeight = KButtonHeight + ((KFontSize + KTextPaddingV) * n) + 3;
    pixWidth = (2*KTextPaddingH) + (KFontSize * maxLength);
    
    genericFont = app.createFont("SansSerif", 14);
    titleFont = app.createFont("SansSerif",18);
  }
  
  public boolean isOpened() {
    return opened;
  }
  
  public int getHeight() {
    return KButtonHeight;
  }
  public int getWidth() {
	return pixWidth;
  }
  public void drawMenu() {
    app.textFont(genericFont);
    app.fill(128);
    app.stroke(0);
    app.strokeWeight(1);
    app.rectMode(app.CORNERS);
    if (opened) {
      app.rect(x,y,x+pixWidth,y+pixHeight);
      for (int ii=0; ii < n; ii++) {
        app.fill(255);
        app.textAlign(app.TOP,app.TOP);
        app.text(data[top+ii],x+KTextPaddingH,y+KButtonHeight + KTextPaddingV + (ii*(KTextPaddingV+KFontSize)));
      }
      // Draw Bar Container
      app.fill(100);
      app.rect(x+pixWidth-KBarWidth,y+KButtonHeight,x+pixWidth,y+pixHeight);
      app.fill(128);
      app.noFill();
      app.strokeWeight(2);
      app.stroke(80);
      app.rect(x+pixWidth-KBarWidth, y+pixHeight-KBarWidth, x+pixWidth, y+pixHeight);
      app.rect(x+pixWidth-KBarWidth, y+KButtonHeight,   x+pixWidth, y+KButtonHeight+KBarWidth);
      app.fill(220);
      app.ellipse(x+pixWidth - (KBarWidth/2), app.map(bottom, 0,dataCount-1, y+KButtonHeight + KBarWidth, y+pixHeight - KBarWidth - KBarWidth/2),KBarWidth,KBarWidth);
      app.noStroke();
    } else {
      app.rect(x,y,x+pixWidth, y+KButtonHeight );
    }
    
    // Draw Title
    app.fill(0);
    app.textFont(titleFont);
    app.textAlign(app.LEFT,app.TOP);
    app.text(title,x+KTextPaddingH,y+(2*KTextPaddingV));
  }
  
  String menuClicked() {
    if ( (app.mouseX > x) && (app.mouseX < x + pixWidth - KBarWidth)) // MenuItemClicked
    {
      if ( (app.mouseY < y+KButtonHeight) && (app.mouseY > y) ) { // Menu Button Clicked
        opened = !opened;
      } else if (app.mouseY < y + pixHeight) {    // Items Area Clicked
        if (opened) {
          int selectedIndex = app.ceil(app.map(app.mouseY,y+KButtonHeight+KTextPaddingH,y+pixHeight,0,n-1));
          opened = false;
          return data[top+selectedIndex];
        }
      }
    } else if (x+app.mouseX < x+pixWidth && x+app.mouseX >  x+pixWidth - KBarWidth) {  // ScrollerClicked
      if (y+app.mouseY < y+KButtonHeight+KBarWidth && y+app.mouseY > y+KButtonHeight ) {
        scrollUp(n-1);
      } else if (y+app.mouseY > y+pixHeight - KBarWidth && y+app.mouseY < y+pixHeight) {
        scrollDown(n-1);
      }
    }
    return null;
  }
  
  void scrollDown(int numLines) {
    if ((bottom + numLines) > dataCount) {
      top += dataCount - bottom;
      bottom = dataCount;
    } else {
      top += numLines;
      bottom+= numLines;
    }
  }
  void scrollUp(int numLines) {
    if ((top - numLines) < 0) {
      bottom -= top;
      top = 0;
    } else {
      top -= numLines;
      bottom-= numLines;
    }
  }
}