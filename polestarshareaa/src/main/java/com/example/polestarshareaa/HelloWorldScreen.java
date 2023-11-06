package com.example.polestarshareaa;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;



public class HelloWorldScreen extends Screen {
    public HelloWorldScreen(CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {

        int speed = 50;
        String str = speed + " km/h";
        int accelerate = 25;
        String str2 = accelerate + " km/h";
        String brakeBehavior = "Calm";



        Row row = new Row.Builder().setTitle("Speed").addText(str).build();
        Row row2 = new Row.Builder().setTitle("Acceleration").addText(str2).build();
        Row row3 = new Row.Builder().setTitle("Brake behavoir").addText(brakeBehavior).build();

        return new PaneTemplate.Builder(new Pane.Builder().addRow(row).addRow(row2).addRow(row3).build()).setHeaderAction(Action.APP_ICON).setTitle("Polestar Share").build();

    }

}


