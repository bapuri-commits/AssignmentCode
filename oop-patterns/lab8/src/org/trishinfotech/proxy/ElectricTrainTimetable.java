package org.trishinfotech.proxy;

public class ElectricTrainTimetable implements TrainTimetable {

    // 디스크에서 읽어온 시간표 데이터
    private String[] timetable;

    public ElectricTrainTimetable() {
        // 생성자에서 디스크 접근 (느린 작업)
        loadFromDisk();
    }

    private void loadFromDisk() {
        System.out.println("디스크에서 시간표를 읽어오는 중... (매우 느림)");
        try {
            Thread.sleep(3000);  // 느린 디스크 접근 시뮬레이션
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 실제로는 레거시 시스템 디스크에서 파일 읽기
        timetable = new String[] {
                "KTX001 06:00 서울→부산",
                "KTX002 06:30 서울→부산",
                "KTX003 07:00 서울→부산",
                "ITX101 07:15 서울→강릉",
                // ...
        };
    }

    @Override
    public String[] getTimetable() {
        // 매번 호출될 때마다 디스크 재접근 (병목 지점!)
        loadFromDisk();
        return timetable;
    }

    @Override
    public String getTrainDepartureTime(String trainId) {
        // 마찬가지로 매번 디스크 접근
        loadFromDisk();
        for (String entry : timetable) {
            if (entry.startsWith(trainId)) {
                return entry.split(" ")[1];  // 시간 부분 추출
            }
        }
        return null;
    }
}