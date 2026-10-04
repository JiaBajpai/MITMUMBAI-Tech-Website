/* =========================================
   CLOCK
========================================= */

function updateClock() {
    const clock = document.getElementById("clock");

    if (!clock) {
        return;
    }

    const now = new Date();

    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");

    clock.textContent = `${hours}:${minutes}`;
}

updateClock();
setInterval(updateClock, 1000);


/* =========================================
   DESKTOP
========================================= */

const desktop = document.querySelector(".desktop");
const folders = document.querySelectorAll(".desktop-icon");


/* =========================================
   DEFAULT FOLDER POSITIONS
========================================= */

const defaultPositions = {
    "about-folder": { x: 20, y: 55 },
    "domains-folder": { x: 20, y: 165 },
    "contact-folder": { x: 20, y: 275 }
};

function positionFolders() {
    folders.forEach(folder => {
        const position = defaultPositions[folder.id];

        if (!position) {
            return;
        }

        folder.style.left = `${position.x}px`;
        folder.style.top = `${position.y}px`;
    });
}

positionFolders();


/* =========================================
   WINDOW SYSTEM
========================================= */

const folderWindows = {
    "about-folder": "about-window",
    "domains-folder": "domains-window",
    "events-folder": "events-window",
    "contact-folder": "contact-window"
};

let highestZIndex = 100;


/* =========================================
   OPEN WINDOW
========================================= */

function openWindow(windowId) {
    const windowElement = document.getElementById(windowId);

    if (!windowElement) {
        return;
    }

    windowElement.style.display = "block";

    highestZIndex++;
    windowElement.style.zIndex = highestZIndex;

    windowElement.classList.remove("minimized");
}


/* =========================================
   CLOSE WINDOW
========================================= */

function closeWindow(windowElement) {
    windowElement.style.display = "none";
}


/* =========================================
   FOLDER DOUBLE CLICK
========================================= */

Object.entries(folderWindows).forEach(([folderId, windowId]) => {
    const folder = document.getElementById(folderId);

    if (!folder) {
        return;
    }

    folder.addEventListener("dblclick", function () {
        openWindow(windowId);
    });
});


/* =========================================
   FOLDER SELECTION
========================================= */

folders.forEach(folder => {
    folder.addEventListener("click", function () {
        folders.forEach(otherFolder => {
            otherFolder.classList.remove("selected");
        });

        folder.classList.add("selected");
    });
});


/* =========================================
   WINDOW FOCUS
========================================= */

document.querySelectorAll(".app-window").forEach(windowElement => {
    windowElement.addEventListener("mousedown", function () {
        highestZIndex++;

        windowElement.style.zIndex = highestZIndex;
    });
});


/* =========================================
   CLOSE BUTTONS
========================================= */

document.querySelectorAll(".app-window").forEach(windowElement => {
    const closeButton =
        windowElement.querySelector(".window-btn.close");

    closeButton.addEventListener("click", function (event) {
        event.stopPropagation();

        closeWindow(windowElement);
    });
});


/* =========================================
   MINIMIZE BUTTONS
========================================= */

document.querySelectorAll(".app-window").forEach(windowElement => {
    const minimizeButton =
        windowElement.querySelector(".window-btn.minimize");

    minimizeButton.addEventListener("click", function (event) {
        event.stopPropagation();

        windowElement.style.display = "none";
    });
});


/* =========================================
   MAXIMIZE BUTTONS
========================================= */

document.querySelectorAll(".app-window").forEach(windowElement => {
    const maximizeButton =
        windowElement.querySelector(".window-btn.maximize");

    maximizeButton.addEventListener("click", function (event) {
        event.stopPropagation();

        if (windowElement.classList.contains("maximized")) {
            windowElement.classList.remove("maximized");

            windowElement.style.width = "";
            windowElement.style.height = "";

            windowElement.style.left = "";
            windowElement.style.top = "";

            windowElement.style.transform =
                "translate(-50%, -50%)";
        } else {
            windowElement.classList.add("maximized");

            windowElement.style.width =
                "calc(100vw - 40px)";

            windowElement.style.height =
                "calc(100vh - 70px)";

            windowElement.style.left = "50%";
            windowElement.style.top = "50%";

            windowElement.style.transform =
                "translate(-50%, -50%)";
        }
    });
});


/* =========================================
   WINDOW DRAGGING
========================================= */

document.querySelectorAll(".app-window").forEach(windowElement => {
    const header =
        windowElement.querySelector(".window-header");

    let dragging = false;
    let offsetX = 0;
    let offsetY = 0;

    header.addEventListener("pointerdown", function (event) {
        if (
            event.target.classList.contains("window-btn")
        ) {
            return;
        }

        if (
            windowElement.classList.contains("maximized")
        ) {
            return;
        }

        dragging = true;

        highestZIndex++;
        windowElement.style.zIndex = highestZIndex;

        const rect =
            windowElement.getBoundingClientRect();

        offsetX =
            event.clientX - rect.left;

        offsetY =
            event.clientY - rect.top;

        header.setPointerCapture(event.pointerId);
    });

    header.addEventListener("pointermove", function (event) {
        if (!dragging) {
            return;
        }

        const desktopRect =
            desktop.getBoundingClientRect();

        let x =
            event.clientX -
            desktopRect.left -
            offsetX;

        let y =
            event.clientY -
            desktopRect.top -
            offsetY;

        const rect =
            windowElement.getBoundingClientRect();

        const maxX =
            desktopRect.width -
            rect.width;

        const maxY =
            desktopRect.height -
            rect.height;

        x = Math.max(
            0,
            Math.min(x, maxX)
        );

        y = Math.max(
            32,
            Math.min(y, maxY)
        );

        windowElement.style.left = `${x}px`;
        windowElement.style.top = `${y}px`;

        windowElement.style.transform = "none";
    });

    header.addEventListener("pointerup", function (event) {
        dragging = false;

        try {
            header.releasePointerCapture(event.pointerId);
        } catch (error) {
            // Pointer already released.
        }
    });
});


/* =========================================
   EVENTS DOCK BUTTON
========================================= */

const eventsButton =
    document.getElementById("events");

if (eventsButton) {
    eventsButton.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();

        openWindow("events-window");
    });
}


/* =========================================
   TERMINAL
========================================= */

const terminalButton =
    document.getElementById("terminal");

const terminalWindow =
    document.getElementById("terminal-window");

const terminalForm =
    document.getElementById("terminal-form");

const terminalInput =
    document.getElementById("terminal-input");

const terminalOutput =
    document.getElementById("terminal-output");


/* =========================================
   TERMINAL OPEN
========================================= */

if (terminalButton) {
    terminalButton.addEventListener("click", function () {
        openWindow("terminal-window");

        setTimeout(function () {
            if (terminalInput) {
                terminalInput.focus();
            }
        }, 50);
    });
}


/* =========================================
   TERMINAL COMMANDS
========================================= */

function printTerminal(text, className = "") {
    if (!terminalOutput) {
        return;
    }

    const line = document.createElement("div");

    if (className) {
        line.className = className;
    }

    line.innerHTML = text;

    terminalOutput.appendChild(line);

    terminalOutput.scrollTop =
        terminalOutput.scrollHeight;
}


function runCommand(command) {
    if (!terminalOutput) {
        return;
    }

    command = command.trim();

    if (!command) {
        return;
    }

    printTerminal(
        `<span class="terminal-green">guest@kernel</span>:<span class="terminal-blue">~</span>$ ${escapeHtml(command)}`
    );

    const lowerCommand =
        command.toLowerCase();

    /* HELP */

    if (lowerCommand === "help") {
        printTerminal(
            `<span class="terminal-white">Available commands:</span>`
        );

        printTerminal(
            `<span class="terminal-white">help</span> - Show available commands`
        );

        printTerminal(
            `<span class="terminal-white">clear</span> - Clear terminal`
        );

        printTerminal(
            `<span class="terminal-white">date</span> - Show current date`
        );

        printTerminal(
            `<span class="terminal-white">time</span> - Show current time`
        );

        printTerminal(
            `<span class="terminal-white">whoami</span> - Show current user`
        );

        printTerminal(
            `<span class="terminal-white">about</span> - About MIT TECH KERNEL`
        );

        printTerminal(
            `<span class="terminal-white">events</span> - Open events`
        );

        return;
    }


    /* CLEAR */

    if (lowerCommand === "clear") {
        terminalOutput.innerHTML = "";

        return;
    }


    /* DATE */

    if (lowerCommand === "date") {
        printTerminal(
            new Date().toLocaleDateString()
        );

        return;
    }


    /* TIME */

    if (lowerCommand === "time") {
        printTerminal(
            new Date().toLocaleTimeString()
        );

        return;
    }


    /* WHOAMI */

    if (lowerCommand === "whoami") {
        printTerminal(
            "guest"
        );

        return;
    }


    /* ABOUT */

    if (lowerCommand === "about") {
        printTerminal(
            "MIT TECH KERNEL"
        );

        printTerminal(
            "Technical Club of MIT Mumbai."
        );

        return;
    }


    /* EVENTS */

    if (lowerCommand === "events") {
        openWindow("events-window");

        return;
    }


    /* UNKNOWN */

    printTerminal(
        `Command not found: ${escapeHtml(command)}`,
        "terminal-yellow"
    );

    printTerminal(
        `Type <span class="terminal-white">help</span> for available commands.`
    );
}


/* =========================================
   ESCAPE HTML
========================================= */

function escapeHtml(text) {
    const div =
        document.createElement("div");

    div.textContent = text;

    return div.innerHTML;
}


/* =========================================
   TERMINAL INPUT
========================================= */

if (terminalForm) {
    terminalForm.addEventListener(
        "submit",
        function (event) {
            event.preventDefault();

            const command =
                terminalInput.value;

            runCommand(command);

            terminalInput.value = "";
        }
    );
}


/* =========================================
   CLICK TERMINAL WINDOW
========================================= */

if (terminalWindow) {
    terminalWindow.addEventListener(
        "click",
        function () {
            if (terminalInput) {
                terminalInput.focus();
            }
        }
    );
}


/* =========================================
   WINDOW RESIZE
========================================= */

window.addEventListener("resize", function () {
    positionFolders();
});


/* =========================================
   KERNEL CALENDAR — IOS AGENDA
========================================= */

const calendarTitle =
    document.getElementById("calendar-title");

const calendarEventsList =
    document.getElementById("calendar-events-list");

const previousMonthButton =
    document.getElementById("previous-month");

const nextMonthButton =
    document.getElementById("next-month");


/* =========================================
   EVENT DATA
========================================= */

const calendarEvents = [
    {
        date: "2026-09-04",
        title: "SIH Internal Hackathon",
        description:
            "College internal round of the Smart India Hackathon (SIH), where participating teams develop and present their solutions for selection to the next stage.",
        time: "all-day"
    },

    {
        date: "2026-09-30",
        title: "Engineers' Day",
        description:
            "A technical celebration featuring six events: BuildX, Chess, Ideathon, RapidResearch, Debate and BuildX Debugging.",
        time: "all-day"
    },

    {
        date: "2026-10-10",
        title: "Techsphere",
        description:
            "An internal showcase for second-year students to present and demonstrate their semester mini projects.",
        time: "all-day"
    }
];


/* =========================================
   CALENDAR STATE
========================================= */

let calendarMonthDate =
    new Date(2026, 8, 1);


/* =========================================
   MONTH NAME
========================================= */

function getMonthName(date) {
    return date.toLocaleDateString(
        "en-US",
        {
            month: "long",
            year: "numeric"
        }
    );
}


/* =========================================
   FORMAT DATE
========================================= */

function parseCalendarDate(dateString) {
    const [
        year,
        month,
        day
    ] = dateString.split("-").map(Number);

    return new Date(
        year,
        month - 1,
        day
    );
}


/* =========================================
   FORMAT EVENT DATE
========================================= */

function getEventDay(dateString) {
    return parseCalendarDate(dateString).getDate();
}


function getEventWeekday(dateString) {
    return parseCalendarDate(dateString)
        .toLocaleDateString(
            "en-US",
            {
                weekday: "short"
            }
        );
}


function getEventHeading(dateString) {
    return parseCalendarDate(dateString)
        .toLocaleDateString(
            "en-US",
            {
                weekday: "long",
                month: "short",
                day: "numeric"
            }
        );
}


/* =========================================
   RENDER CALENDAR
========================================= */

function renderCalendarEvents() {
    if (
        !calendarTitle ||
        !calendarEventsList
    ) {
        return;
    }

    const year =
        calendarMonthDate.getFullYear();

    const month =
        calendarMonthDate.getMonth();

    /* Title */

    calendarTitle.textContent =
        getMonthName(calendarMonthDate);

    /* Clear */

    calendarEventsList.innerHTML = "";

    /* Filter events for current month */

    const monthEvents =
        calendarEvents.filter(event => {
            const date =
                parseCalendarDate(event.date);

            return (
                date.getFullYear() === year &&
                date.getMonth() === month
            );
        });

    /* No events */

    if (monthEvents.length === 0) {
        calendarEventsList.innerHTML = `
            <div class="calendar-empty">
                No MIT TECH KERNEL events this month.
            </div>
        `;

        return;
    }

    /* CREATE DATE GROUPS */

    monthEvents.forEach(event => {
        const dateGroup =
            document.createElement("div");

        dateGroup.className =
            "calendar-date-group";

        /* Date heading */

        const heading =
            document.createElement("div");

        heading.className =
            "calendar-date-heading";

        heading.textContent =
            getEventHeading(event.date);

        dateGroup.appendChild(heading);

        /* Event row */

        const eventRow =
            document.createElement("button");

        eventRow.type = "button";

        eventRow.className =
            "calendar-event-row";

        eventRow.innerHTML = `
            <div class="calendar-event-date">
                <span class="calendar-event-day">
                    ${getEventDay(event.date)}
                </span>

                <span class="calendar-event-weekday">
                    ${getEventWeekday(event.date)}
                </span>
            </div>

            <div class="calendar-event-info">
                <span class="calendar-event-name">
                    ${event.title}
                </span>

                <span class="calendar-event-time">
                    ${event.time}
                </span>
            </div>

            <span class="calendar-event-arrow">
                ›
            </span>
        `;

        /* Description */

        const description =
            document.createElement("div");

        description.className =
            "calendar-event-description";

        description.innerHTML = `
            <strong>${event.title}</strong>
            ${event.description}
        `;

        /* EVENT CLICK */

        eventRow.addEventListener(
            "click",
            function () {
                const alreadySelected =
                    eventRow.classList.contains("selected");

                /* Close everything */

                document
                    .querySelectorAll(".calendar-event-row")
                    .forEach(row => {
                        row.classList.remove("selected");
                    });

                document
                    .querySelectorAll(".calendar-event-description")
                    .forEach(desc => {
                        desc.classList.remove("visible");
                    });

                /* Open selected */

                if (!alreadySelected) {
                    eventRow.classList.add("selected");

                    description.classList.add("visible");
                }
            }
        );

        dateGroup.appendChild(eventRow);
        dateGroup.appendChild(description);

        calendarEventsList.appendChild(dateGroup);
    });
}


/* =========================================
   PREVIOUS MONTH
========================================= */

if (previousMonthButton) {
    previousMonthButton.addEventListener(
        "click",
        function () {
            calendarMonthDate.setMonth(
                calendarMonthDate.getMonth() - 1
            );

            renderCalendarEvents();
        }
    );
}


/* =========================================
   NEXT MONTH
========================================= */

if (nextMonthButton) {
    nextMonthButton.addEventListener(
        "click",
        function () {
            calendarMonthDate.setMonth(
                calendarMonthDate.getMonth() + 1
            );

            renderCalendarEvents();
        }
    );
}


/* =========================================
   INITIALIZE
========================================= */

renderCalendarEvents();