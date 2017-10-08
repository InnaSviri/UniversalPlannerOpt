package ru.programpark.entity.data;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 01.04.15
 * Time: 16:39
 * To change this template use File | Settings | File Templates.
 */
public class TeamLocoSummaryData {
    public int n_loco_attr_no_fact_loco = 0;//строка 18
    public int n_loco_attr_no_next_service = 0;//строка 19
    public int n_team_attr_no_fact_team = 0;//строка 20
    public int n_team_attr_no_next_rest = 0;//строка 21

    public TeamLocoSummaryData(int n_loco_attr_no_fact_loco, int n_loco_attr_no_next_service, int n_team_attr_no_fact_team, int n_team_attr_no_next_rest) {
        this.n_loco_attr_no_fact_loco = n_loco_attr_no_fact_loco;
        this.n_loco_attr_no_next_service = n_loco_attr_no_next_service;
        this.n_team_attr_no_fact_team = n_team_attr_no_fact_team;
        this.n_team_attr_no_next_rest = n_team_attr_no_next_rest;
    }
}
