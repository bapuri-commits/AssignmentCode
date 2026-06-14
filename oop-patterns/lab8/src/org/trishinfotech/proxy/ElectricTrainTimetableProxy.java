package org.trishinfotech.proxy;

public class ElectricTrainTimetableProxy implements TrainTimetable {

    private TrainTimetable trainTimetable = new ElectricTrainTimetable();

    private String[] timetableCache = null;

    @Override
    public String[] getTimetable() {
        if(timetableCache == null)timetableCache = trainTimetable.getTimetable();
        return timetableCache;
    }

    @Override
    public String getTrainDepartureTime(String trainId) {
        return trainTimetable.getTrainDepartureTime(trainId);//여기도 적용해볼수 있으나 원본 테이블이 어떻게 생겼는지 잘 모르기에 건들지 않음.
    }

    public void clearCache() {
        timetableCache = null;
    }
}