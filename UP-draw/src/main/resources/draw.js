
    var PADDING = 30;

    function _ (name, attrs) {
        var elt = (typeof name == 'string')
            ? document.createElement(name)
            : document.createElementNS(name[0], name[1]);
        if (attrs) {
            for (attr in attrs) {
                if (attrs.hasOwnProperty(attr))
                    elt.setAttribute(attr, attrs[attr]);
            }
        }
        for (var i = 2; i < arguments.length; ++i) {
            var arg = arguments[i];
            elt.appendChild((typeof arg == 'string' || typeof arg == 'number' || ! arg)
                                ? document.createTextNode(arg)
                                : _.apply(this, arg));
        }
        return elt;
    }

    function copy (a, b, weak) {
        for (prop in a) {
            if (a.hasOwnProperty(prop) && ! (weak && b.hasOwnProperty(prop)))
                b[prop] = a[prop];
        }
        return b;
    }

    function strset (str, x, mode) {
        var arr = str ? str.split(/ +/) : [];
        var i = arr.indexOf(x);
        if (mode) {
            if (i < 0) arr.push(x);
        } else {
            if (i >= 0) arr.splice(i, 1);
        }
        return arr.join(' ');
    };

    var Train = {

        desc: function () {
            return this.num || this.id;
        },

        sTrack: function (i) {
            if (this.rev) {
                i = this.i0 - i;
                var direct = this.stretch.route[i];
                return copy(direct, { to: direct.from, from: direct.to }, true);
            } else {
                i = this.i0 + i;
                return this.stretch.route[i];
            }
        },

        routeDesc: function () {
            var start = this.sTrack(0).from,
                end = this.sTrack(this.route.length - 1).to;
            return (start.name || start.id) + " → " + (end.name || end.id);
        },

        routeReduce: function (fn, init) {
            var s0, stFrom, stTo;
            for (var i = 0; i < this.route.length; ++i) {
                init = fn(init, this.route[i], this.sTrack(i));
            }
            return init;
        },

        hover: function (mode) {
            this.svg.attr({'class': strset(this.svg.attr('class'), 'hover', mode)});
            this.handle.className = strset(this.handle.className, 'hover', mode);
        },

        toggle: function () {
            this.highlight = ! this.highlight;
            this.svg.attr({'class': strset(this.svg.attr('class'), 'highlight', this.highlight)});
            this.handle.className = strset(this.handle.className, 'highlight', this.highlight);
        },

        setupEvents: function (node) {
            var train = this;
            node.onclick = function () { train.toggle(); };
            node.onmouseover = function () { train.hover(true); };
            node.onmouseout = function () { train.hover(false); };
        }

    };

    var GridSelector = {

        toggle: function () {
            if (this.trains) {
                var highlight = ! this.highlight;
                if (this.trains.every(function (train) {
                            return train.highlight == highlight;
                        }))
                    highlight = ! highlight;
                this.trains.forEach(function (train) {
                    if (train.highlight != highlight)
                        train.toggle();
                });
                this.highlight = highlight;
            }
        },

        setupEvents: function (node) {
            var sel = this;
            node.onclick = function () { sel.toggle(); }
        }

    };

    var Stretch = {

        extend : function (datum) {
            datum.totalDistance = 0;
            for (var i = -1, d = 0; i < datum.route.length; ++i) {
                var st;
                if (i > 0) {
                    datum.route[i].from = st;
                }
                if (i < 0) {
                    st = datum.route[0].from;
                } else {
                    st = datum.route[i].to;
                    datum.totalDistance += datum.route[i].dist;
                }
                st.dOffset = datum.totalDistance;
            }
            datum.tMin = datum.range.start;
            datum.tMax = datum.range.end;
            datum.trains.sort(function (a, b) {
                return (a.num && b.num) ? (a.num - b.num) : (a.id - b.id);
            });
            datum.trains = datum.trains.map(function (train) {
                datum.tMin = Math.min(datum.tMin, train.t0);
                if (train.ready) datum.tMin = Math.min(datum.tMin, train.ready.t);
                var tEndMax = function (tMax, trk) { return Math.max(tMax, trk[1]); };
                datum.tMax = train.route.reduce(tEndMax, datum.tMax);
                return copy(train, Object.create(Train, {stretch: {value: datum}}));
            });
            return copy(Stretch, datum);
        },

        stationReduce : function (fn, value) {
            for (var i = -1; i < this.route.length; ++i) {
                var st = (i < 0) ? this.route[0].from : this.route[i].to;
                value = fn(value, st);
            }
            return value;
        },

        makeSVG : function () {
            var mainDiv = document.getElementById('main-' + this.stretch);
            var menuDiv = document.getElementById('menu-' + this.stretch);
            this.height = Math.max(Math.round(Math.log(this.totalDistance) * 100),
                                   menuDiv.offsetHeight);
            this.width = mainDiv.offsetWidth;
            this.svg = SVG(mainDiv).size(this.width, this.height);
            var parent = mainDiv.parentElement;
            while (parent != document.body && parent.offsetHeight < mainDiv.offsetHeight) {
                parent.style.height = mainDiv.offsetHeight + 'px';
                parent = parent.parentElement;
            }
            menuDiv.style.top = mainDiv.offsetTop + "px";
            menuDiv.style.height = mainDiv.offsetHeight + "px";
            return this;
        },

        makeMenu : function () {
            var menuDiv = document.getElementById('menu-' + this.stretch);
            var p = _('p', {'class': 'trains'});
            menuDiv.appendChild(_('h3', {}, 'Поезда'));
            menuDiv.appendChild(p);
            this.trains.forEach(function (train) {
                train.handle = _('span', {'id': 'menu-train-' + train.id},
                                 train.desc() + ": " + train.routeDesc());
                train.setupEvents(train.handle);
                p.appendChild(train.handle);
                p.appendChild(_('br'));
            });
            return this;
        },

        timeToX : function (t) {
            return this.left +
                (this.right - this.left) *
                    (t - this.tMin) / (this.tMax - this.tMin);
        },

        appendTitle : function (node, content) {
            node.appendChild(_([node.namespaceURI, 'title'], {}, content));
        },

        drawGrid : function () {
            var top = this.top = PADDING * 2;
            var bottom = this.bottom = this.height - PADDING;
            var dTotal = this.totalDistance;
            var gridGroup = this.svg.group();
            gridGroup.attr({class: "grid"});

            // Станции, проход 1 (определение макс. ширины имени)
            var maxNameWidth = this.stationReduce(function (max, st) {
                st.svg = { y: top + (bottom - top) * st.dOffset / dTotal };
                if (st.isLocoExch || st.isTeamExch) {
                    st.svg.cap = gridGroup.plain(st.name);
                    return Math.max(st.svg.cap.length(), max);
                } else {
                    return max;
                }
            }, 0);
            var left = this.left = PADDING + maxNameWidth;
            var right = this.right = this.width - PADDING * 2;

            // Часовые засечки
            var hrMarks = this.hrMarks = new Array();
            hrMarks.addMark = function (t, x) {
                var mark = { date: new Date(t * 1000),
                             svg: { x: x, lines: new Array() } };
                this.push(mark);
                return mark;
            };
            var t, mark;
            for (t = Math.floor(this.tMin / 3600) * 3600; t <= this.tMax; t += 3600) {
                if (t < this.tMin) continue;
                mark = hrMarks.addMark(t, this.timeToX(t));
                var boundary = (t == this.range.start || t == this.range.end) ? 4 :
                    (mark.date.getHours() == 18) ? 3 :
                    ((t - this.range.start) % this.range.locoFrame == 0) ? 2 :
                    ((t - this.range.start) % this.range.teamFrame == 0) ? 1 :
                    0;
                if (boundary > 0) {
                    var y0 = top - (PADDING - 10) / 2 + 5;
                    var y1 = bottom + 2.5;
                    var addLine = function (x, cls) {
                        var line = gridGroup.line(x, y0, x, y1);
                        line.attr({ 'class': 'hr-mark-' + cls });
                        mark.svg.lines.push(line);
                    }
                    if (boundary == 4) addLine(mark.svg.x, 'highlight');
                    if (boundary >= 3) {
                        addLine(mark.svg.x - 1, 'day');
                        addLine(mark.svg.x + 1, 'day');
                    } else if (boundary == 2) {
                        addLine(mark.svg.x, 'lframe');
                    } else if (boundary == 1) {
                        addLine(mark.svg.x, 'tframe');
                    }
                    var fmt = ("0" + mark.date.getHours()).substr(-2) + ":" +
                        ("0" + mark.date.getMinutes()).substr(-2);
                    mark.svg.cap = gridGroup.plain(fmt);
                    mark.svg.cap.attr({ 'x': mark.svg.x, 'y': y0 - 5,
                                        'dominant-baseline': 'central'});
                    mark.svg.cap.rotate(270, mark.svg.x, y0 - 5);
                    if (boundary > 1 && t > this.range.start)
                        mark.grSels = new Object();
                }
            }

            // Станции, проход 2 (выравнивание подписей, отрисовка линий, селекторы)
            this.stationReduce(function (stretch, st) {
                if (st.svg.cap) {
                    st.svg.line = gridGroup.line(left - 5, st.svg.y, right + 5, st.svg.y);
                    st.svg.line.attr({ class: (st.isLocoExch ? 'loco-exch' : 'team-exch') });
                    st.svg.cap.attr({ 'x': left - 15, 'y': st.svg.y, 'text-anchor': 'end',
                                      'dominant-baseline' : 'central' });
                    st.grSels = new Array();
                    for (var i = 0, frm = 0, t_ = stretch.tMin; i < hrMarks.length; ++i) {
                        mark = hrMarks[i];
                        t = mark.date.valueOf() / 1000;
                        if (mark.grSels && t > t_) {
                            var sel = Object.create(GridSelector);
                            sel.station = st;
                            sel.frame = ++frm;
                            sel.hrMark = mark;
                            sel.trains = new Array();
                            mark.grSels[st.id] = sel;
                            st.grSels[frm] = sel;
                            sel.svg = { x1: stretch.timeToX(t_), x2: mark.svg.x,
                                        y1: st.svg.y - 10, y2: st.svg.y + 10 };
                            sel.svg.rect =
                                gridGroup.rect(sel.svg.x2 - sel.svg.x1,
                                               sel.svg.y2 - sel.svg.y1)
                                         .attr({ 'x': sel.svg.x1, 'y': sel.svg.y1,
                                                 'class': 'sel' });
                            sel.setupEvents(sel.svg.rect.node);
                            t_ = t;
                        }
                    }
                }
                return stretch;
            }, this);

            return this;
        },

        drawTrains : function () {
            var trainsGroup = this.svg.group();
            trainsGroup.attr({class: "trains"});
            this.trains.reduce(function(stretch, train) {
                var frm = null;
                var points = train.routeReduce(function (points, tTrack, sTrack) {
                    var sels = sTrack.from.grSels;
                    var sel = sels && sels[tTrack[0]];
                    if (sel && sel.frame !== frm) {
                        frm = sel.frame;
                        sel.trains.push(train);
                    }
                    var pts = [[stretch.timeToX(tTrack[1]), sTrack.from.svg.y],
                               [stretch.timeToX(tTrack[2]), sTrack.to.svg.y]];
                    return points.concat(pts);
                }, new Array());
                train.svg = trainsGroup.polyline(points).fill('none');
                train.svg.attr({ 'class': train.route[0][4].toLowerCase() });
                stretch.appendTitle(train.svg.node, train.desc());
                train.setupEvents(train.svg.node);
                return stretch;
            }, this);
            return this;
        }
    };

    function checkTab (index) {
        var checked = (index == null);
        for (var i = 1; i <= STRETCHES.length; ++i) {
            var ear = document.getElementById("ear" + i);
            if (ear) {
                if (index == null) {
                    ear.onclick = function () {
                        checkTab(parseInt(this.id.substr(3)));
                    };
                } else {
                    ear.checked = (i == index);
                }
                if (!checked) checked = ear.checked;
            }
        }
        return checked;
    }

    if (checkTab()) {
        for (var i = 0; i < STRETCHES.length; ++i) {
            checkTab(i + 1);
            Stretch.extend(STRETCHES[i]).makeSVG().makeMenu()
                   .drawGrid().drawTrains();
        }
        checkTab(1);
    }

    /*
        private float[] DASH_3_2 = {3f, 2f};
        private float[] DASH_5_3 = {5f, 3f};
        private float[] DOT_1_2 = {.5f, 1f};
        private Stroke THIN_SOLID = new BasicStroke(.5f);
        private Stroke THIN_DASHED = new BasicStroke(.5f,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f,
                DASH_3_2, 1.5f);
        private Stroke THIN_DOTTED = new BasicStroke(.5f,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f,
                DOT_1_2, 0f);
        private Stroke MEDIUM_SOLID = new BasicStroke(1.2f);
        private Stroke MEDIUM_DASHED = new BasicStroke(1.2f,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f,
                DASH_5_3, 2.5f);
        private Stroke THICK_SOLID = new BasicStroke(3f);
        private Stroke THICK_DASHED = new BasicStroke(3f,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f,
                DASH_5_3, 2.5f);

        void drawLocos(SVGGraphics2D svg) {
            Set<Loco> locos = new HashSet<>();
            for (Train train : trains) {
                for (Train.EventContainer track : train.getRoute()) {
                    AssignEvent aEvt = track.lastEvent(AssignEvent.class);
                    if (aEvt == null) continue;
                    Long id = aEvt.getLocoId();
                    if (id != null)
                        locos.add(data.getLoco(id));
                }
            }
            String badLocos = null;
            for (Loco loco : locos) {
                LinkedHashMap<Points, Stroke> ppMap = new LinkedHashMap<>();
                Points curPoints = null;
                Stroke curStrk = null;
                try {
                    for (Loco.Event evt : loco.allEvents()) {
                        if (evt instanceof LocoAssignEvent) {
                            LocoAssignEvent lEvt = (LocoAssignEvent) evt;
                            Stroke strk = lEvt.isRelocation() ?
                                MEDIUM_DASHED : MEDIUM_SOLID;
                            if (strk != curStrk || curPoints == null)
                                ppMap.put((curPoints = new Points(curPoints)),
                                          (curStrk = strk));
                            curPoints.extend(lEvt);
                        } else if (evt instanceof LocoServiceEvent) {
                            LocoServiceEvent sEvt = (LocoServiceEvent) evt;
                            if (sEvt.getStationId() < 0L) continue;
                            Station st = SchedulingData.getStation(sEvt.getStationId());
                            long t1 = sEvt.getStartTime(), t2 = sEvt.getEndTime();
                            if (curPoints != null)
                                curPoints.extend(t1, st, -2);
                            if (curStrk != MEDIUM_DASHED || curPoints == null)
                                ppMap.put((curPoints = new Points(curPoints)),
                                          (curStrk = MEDIUM_DASHED));
                            curPoints.extend(t2, st, -2);
                        } else {
                            // ...
                        }
                    }
                    for (Map.Entry<Points, Stroke> ppStrk : ppMap.entrySet()) {
                        float r = (float) Math.random();
                        svg.setPaint(Color.getHSBColor(r * .1f, 1f, 1f));
                        svg.setStroke(ppStrk.getValue());
                        Points pp = ppStrk.getKey();
                        svg.drawPolyline(pp.x, pp.y, pp.n);
                    }
                } catch (Exception e) {
                    if (badLocos == null) badLocos = "";
                    else badLocos += ", ";
                    badLocos += loco.getId().toString();
                }
            }
            if (badLocos != null)
                System.err.println("Ошибки при отображении локомотивов: " +
                                       badLocos);
        }

    */