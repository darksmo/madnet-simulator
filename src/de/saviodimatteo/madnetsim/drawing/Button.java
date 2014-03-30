package de.saviodimatteo.madnetsim.drawing;

import processing.core.PApplet;
import processing.core.PFont;

public class Button {
	int x = 0;
	int y = 0;
	int w = 100;
	int h = 100;
	String label = "Button";
	PApplet app = null;
	PFont labelFont;
	public Button(PApplet father, int x, int y, int w, int h, String label) {
		app = father;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.label = label;
		labelFont = app.createFont("SansSerif",12);
	}
	
	public int getEndWidth() {
		return this.x + this.w;
	}
	public void drawButton() {
		app.stroke(0);
		app.strokeWeight(1);
		
		app.fill(128);
		app.rectMode(app.CORNERS);
		app.rect(x,y,x+w,y+h);
		
		app.fill(0);
		app.textAlign(app.CENTER,app.CENTER);
		app.textFont(labelFont);
		app.text(label,x+(w/2),y+(h/2));
	}
	
	public boolean clicked(int mouseX, int mouseY) {
		if (mouseX > x && mouseX < x + w) {
			if ( mouseY > y && mouseY < y+h) {
				return true;
			}
		}
		return false;
	}
}
