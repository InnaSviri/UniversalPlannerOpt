package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Link;

/**
 * Date: 29.05.14
 * fact_loco
 * Текущее местонахождение локомотивов на станциях
 +fact_loco(id(123213), fact_time(1349877600),location(station(44587542)))
 LocoId – идентификатор локомотива.
 FactTime – время, на которое актуален данный факт.
 LocStId – идентификатор станции, на которой в данный момент находится локомотив.

 Текущее местонахождение локомотивов на участках
 +fact_loco(id(556464), fact_time(1349877600), location(track(station(231231231), station(3123133), depart_time(1349877600), locoState(0), train(54641))), destination(0))

 LocoId – идентификатор локомотива.
 FactTime – время, на которое актуален данный факт.
 St1 – идентификатор станции, с которой отправился локомотив.
 St2 – идентификатор станции, на которую отправился локомотив (не конечная станция движения локомотива, а конечная станция участка, на который отправился локомотив).
 DepTime – время отправления локомотива со станции St1.
 State – состояние локомотива (0 – следует резервом, 1 – ведет поезд).
 TrainId – идентификатор поезда, к которому прикреплен локомотив.
 Dest – идентификатор конечной станции движения локомотива по текущему плану. Этот параметр известен только для локомотива, следующего резервом без поезда, в остальных случаях он равняется 0.
 */
public class FactLocoTrack extends BaseLocoTrack{
    Long timeDepart;

    public FactLocoTrack() {
    }

    public FactLocoTrack(Link link, State state, Long trainId, Long timeDepart) {
        super(link, state, trainId);
        this.timeDepart = timeDepart;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactLocoTrack");
        sb.append("{timeDepart=").append(timeDepart);
        sb.append('}');
        return sb.toString();
    }

    public Long getTimeDepart() {
        return timeDepart;
    }

    public void setTimeDepart(Long timeDepart) {
        this.timeDepart = timeDepart;
    }
}
