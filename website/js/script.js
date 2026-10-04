/* =========================================
   CLOCK
========================================= */

function updateClock() {

    const clock = document.getElementById("clock");

    if (!clock) {
        return;
    }

    const now = new Date();

    const hours = String(
        now.getHours()
    ).padStart(2, "0");

    const minutes = String(
        now.getMinutes()
    ).padStart(2, "0");

    clock.textContent = `${hours}:${minutes}`;
}


updateClock();

setInterval(updateClock, 1000);


/* =========================================
   DESKTOP
========================================= */

const desktop = document.querySelector(".desktop");

const folders =
    document.querySelectorAll(".desktop-icon");


/* =========================================
   DEFAULT FOLDER POSITIONS
========================================= */

const defaultPositions = {

    "about-folder": {
        x: window.innerWidth - 180,
        y: 70
    },

    "domains-folder": {
        x: window.innerWidth - 180,
        y: 180
    },

    "events-folder": {
        x: window.innerWidth - 180,
        y: 290
    },

    "contact-folder": {
        x: window.innerWidth - 180,
        y: 400
    }

};


/* =========================================
   SAVED POSITIONS
========================================= */

function getSavedPositions() {

    const saved =
        localStorage.getItem(
            "kernel-folder-positions"
        );

    if (!saved) {
        return {};
    }

    try {

        return JSON.parse(saved);

    } catch (error) {

        console.error(
            "Could not load folder positions:",
            error
        );

        return {};
    }
}


function savePositions() {

    const positions = {};

    folders.forEach(folder => {

        positions[folder.id] = {

            x: parseFloat(folder.style.left),

            y: parseFloat(folder.style.top)

        };

    });


    localStorage.setItem(
        "kernel-folder-positions",
        JSON.stringify(positions)
    );
}


/* =========================================
   POSITION FOLDERS
========================================= */

function positionFolders() {

    const savedPositions =
        getSavedPositions();


    folders.forEach(folder => {

        const saved =
            savedPositions[folder.id];

        const defaultPosition =
            defaultPositions[folder.id];


        if (saved) {

            folder.style.left =
                `${saved.x}px`;

            folder.style.top =
                `${saved.y}px`;

        }

        else if (defaultPosition) {

            folder.style.left =
                `${defaultPosition.x}px`;

            folder.style.top =
                `${defaultPosition.y}px`;

        }

    });

}


/* =========================================
   KEEP FOLDER INSIDE DESKTOP
========================================= */

function keepInsideDesktop(
    folder,
    x,
    y
) {

    const desktopRect =
        desktop.getBoundingClientRect();

    const folderRect =
        folder.getBoundingClientRect();


    const maxX =
        desktopRect.width -
        folderRect.width;


    const maxY =
        desktopRect.height -
        folderRect.height;


    const minY = 40;


    x = Math.max(
        0,
        Math.min(x, maxX)
    );


    y = Math.max(
        minY,
        Math.min(y, maxY)
    );


    return {
        x,
        y
    };

}


/* =========================================
   MAKE FOLDER DRAGGABLE
========================================= */

function makeDraggable(folder) {

    let isDragging = false;

    let offsetX = 0;
    let offsetY = 0;


    /* POINTER DOWN */

    folder.addEventListener(
        "pointerdown",
        function (event) {

            if (
                event.button !== 0 &&
                event.pointerType === "mouse"
            ) {
                return;
            }


            isDragging = true;


            folder.setPointerCapture(
                event.pointerId
            );


            folder.classList.add(
                "dragging"
            );


            const rect =
                folder.getBoundingClientRect();


            offsetX =
                event.clientX -
                rect.left;


            offsetY =
                event.clientY -
                rect.top;

        }
    );


    /* POINTER MOVE */

    folder.addEventListener(
        "pointermove",
        function (event) {

            if (!isDragging) {
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


            const position =
                keepInsideDesktop(
                    folder,
                    x,
                    y
                );


            folder.style.left =
                `${position.x}px`;

            folder.style.top =
                `${position.y}px`;

        }
    );


    /* POINTER UP */

    folder.addEventListener(
        "pointerup",
        function (event) {

            if (!isDragging) {
                return;
            }


            isDragging = false;


            folder.classList.remove(
                "dragging"
            );


            try {

                folder.releasePointerCapture(
                    event.pointerId
                );

            } catch (error) {
                // Pointer already released.
            }


            savePositions();

        }
    );


    /* POINTER CANCEL */

    folder.addEventListener(
        "pointercancel",
        function () {

            isDragging = false;

            folder.classList.remove(
                "dragging"
            );

        }
    );

}


/* =========================================
   INITIALIZE FOLDERS
========================================= */

positionFolders();


folders.forEach(folder => {

    makeDraggable(folder);

});


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

    const windowElement =
        document.getElementById(windowId);


    if (!windowElement) {
        return;
    }


    windowElement.style.display = "block";


    highestZIndex++;

    windowElement.style.zIndex =
        highestZIndex;


    windowElement.classList.remove(
        "minimized"
    );

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

Object.entries(folderWindows)
    .forEach(([folderId, windowId]) => {

        const folder =
            document.getElementById(folderId);


        if (!folder) {
            return;
        }


        folder.addEventListener(
            "dblclick",
            function () {

                openWindow(windowId);

            }
        );

    });


/* =========================================
   FOLDER SELECTION
========================================= */

folders.forEach(folder => {

    folder.addEventListener(
        "click",
        function () {

            folders.forEach(
                otherFolder => {

                    otherFolder.classList.remove(
                        "selected"
                    );

                }
            );


            folder.classList.add(
                "selected"
            );

        }
    );

});


/* =========================================
   WINDOW FOCUS
========================================= */

document
    .querySelectorAll(".app-window")
    .forEach(windowElement => {

        windowElement.addEventListener(
            "mousedown",
            function () {

                highestZIndex++;

                windowElement.style.zIndex =
                    highestZIndex;

            }
        );

    });


/* =========================================
   CLOSE BUTTONS
========================================= */

document
    .querySelectorAll(".app-window")
    .forEach(windowElement => {

        const closeButton =
            windowElement.querySelector(
                ".window-btn.close"
            );


        closeButton.addEventListener(
            "click",
            function (event) {

                event.stopPropagation();

                closeWindow(windowElement);

            }
        );

    });


/* =========================================
   MINIMIZE BUTTONS
========================================= */

document
    .querySelectorAll(".app-window")
    .forEach(windowElement => {

        const minimizeButton =
            windowElement.querySelector(
                ".window-btn.minimize"
            );


        minimizeButton.addEventListener(
            "click",
            function (event) {

                event.stopPropagation();

                windowElement.style.display =
                    "none";

            }
        );

    });


/* =========================================
   MAXIMIZE BUTTONS
========================================= */

document
    .querySelectorAll(".app-window")
    .forEach(windowElement => {

        const maximizeButton =
            windowElement.querySelector(
                ".window-btn.maximize"
            );


        maximizeButton.addEventListener(
            "click",
            function (event) {

                event.stopPropagation();


                if (
                    windowElement.classList.contains(
                        "maximized"
                    )
                ) {

                    windowElement.classList.remove(
                        "maximized"
                    );


                    windowElement.style.width = "";
                    windowElement.style.height = "";

                    windowElement.style.left = "";
                    windowElement.style.top = "";

                    windowElement.style.transform =
                        "translate(-50%, -50%)";

                }

                else {

                    windowElement.classList.add(
                        "maximized"
                    );


                    windowElement.style.width =
                        "calc(100vw - 40px)";

                    windowElement.style.height =
                        "calc(100vh - 70px)";

                    windowElement.style.left =
                        "50%";

                    windowElement.style.top =
                        "50%";

                    windowElement.style.transform =
                        "translate(-50%, -50%)";

                }

            }
        );

    });


/* =========================================
   WINDOW DRAGGING
========================================= */

document
    .querySelectorAll(".app-window")
    .forEach(windowElement => {

        const header =
            windowElement.querySelector(
                ".window-header"
            );


        let dragging = false;

        let offsetX = 0;
        let offsetY = 0;


        header.addEventListener(
            "pointerdown",
            function (event) {

                if (
                    event.target.classList.contains(
                        "window-btn"
                    )
                ) {
                    return;
                }


                if (
                    windowElement.classList.contains(
                        "maximized"
                    )
                ) {
                    return;
                }


                dragging = true;


                highestZIndex++;

                windowElement.style.zIndex =
                    highestZIndex;


                const rect =
                    windowElement.getBoundingClientRect();


                offsetX =
                    event.clientX -
                    rect.left;


                offsetY =
                    event.clientY -
                    rect.top;


                header.setPointerCapture(
                    event.pointerId
                );

            }
        );


        header.addEventListener(
            "pointermove",
            function (event) {

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


                windowElement.style.left =
                    `${x}px`;

                windowElement.style.top =
                    `${y}px`;

                windowElement.style.transform =
                    "none";

            }
        );


        header.addEventListener(
            "pointerup",
            function (event) {

                dragging = false;


                try {

                    header.releasePointerCapture(
                        event.pointerId
                    );

                } catch (error) {
                    // Pointer already released.
                }

            }
        );

    });


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


/* OPEN TERMINAL */

if (terminalButton) {

    terminalButton.addEventListener(
        "click",
        function () {

            openWindow("terminal-window");

            setTimeout(() => {

                terminalInput.focus();

            }, 50);

        }
    );

}


/* =========================================
   TERMINAL OUTPUT
========================================= */

function printTerminal(
    text,
    className = ""
) {

    const line =
        document.createElement("div");


    if (className) {

        line.className =
            className;

    }


    line.innerHTML = text;


    terminalOutput.appendChild(line);


    terminalOutput.scrollTop =
        terminalOutput.scrollHeight;

}


/* =========================================
   TERMINAL COMMAND
========================================= */

function runCommand(command) {

    const cleanCommand =
        command.trim().toLowerCase();


    if (!cleanCommand) {
        return;
    }


    /* Show command */

    printTerminal(
        `<span class="terminal-prompt">guest@kernel:~$</span> <span class="terminal-command">${escapeHtml(command)}</span>`
    );


    /* HELP */

    if (cleanCommand === "help") {

        printTerminal(
            "Available commands:",
            "terminal-blue"
        );

        printTerminal(
            "  help       Show available commands"
        );

        printTerminal(
            "  about      Open About Us"
        );

        printTerminal(
            "  domains    Open Technical Domains"
        );

        printTerminal(
            "  events     Open Events"
        );

        printTerminal(
            "  contact    Open Contact Us"
        );

        printTerminal(
            "  whoami     Display current user"
        );

        printTerminal(
            "  date       Display current date"
        );

        printTerminal(
            "  ls         List desktop applications"
        );

        printTerminal(
            "  neofetch   Display KERNEL system information"
        );

        printTerminal(
            "  kernel     Open KERNEL member portal"
        );

        printTerminal(
            "  clear      Clear terminal"
        );

        return;
    }


    /* ABOUT */

    if (cleanCommand === "about") {

        printTerminal(
            "Opening ABOUT US...",
            "terminal-muted"
        );

        openWindow("about-window");

        return;
    }


    /* DOMAINS */

    if (cleanCommand === "domains") {

        printTerminal(
            "Opening DOMAINS...",
            "terminal-muted"
        );

        openWindow("domains-window");

        return;
    }


    /* EVENTS */

    if (cleanCommand === "events") {

        printTerminal(
            "Opening EVENTS...",
            "terminal-muted"
        );

        openWindow("events-window");

        return;
    }


    /* CONTACT */

    if (
        cleanCommand === "contact" ||
        cleanCommand === "contact us"
    ) {

        printTerminal(
            "Opening CONTACT US...",
            "terminal-muted"
        );

        openWindow("contact-window");

        return;
    }


    /* WHOAMI */

    if (cleanCommand === "whoami") {

        printTerminal(
            "guest",
            "terminal-green"
        );

        return;
    }


    /* DATE */

    if (cleanCommand === "date") {

        printTerminal(
            new Date().toString(),
            "terminal-muted"
        );

        return;
    }


    /* LS */

    if (cleanCommand === "ls") {

        printTerminal(
            "ABOUT_US/",
            "terminal-blue"
        );

        printTerminal(
            "DOMAINS/",
            "terminal-blue"
        );

        printTerminal(
            "EVENTS/",
            "terminal-blue"
        );

        printTerminal(
            "CONTACT_US/",
            "terminal-blue"
        );

        printTerminal(
            "KERNEL",
            "terminal-green"
        );

        return;
    }


    /* NEOFETCH */

    if (cleanCommand === "neofetch") {

        printTerminal(
            "       ██╗  ██╗███████╗██████╗ ███╗   ██╗███████╗██╗",
            "terminal-green"
        );

        printTerminal(
            "       ██║ ██╔╝██╔════╝██╔══██╗████╗  ██║██╔════╝██║"
        );

        printTerminal(
            "       █████╔╝ █████╗  ██████╔╝██╔██╗ ██║█████╗  ██║"
        );

        printTerminal(
            "       ██╔═██╗ ██╔══╝  ██╔══██╗██║╚██╗██║██╔══╝  ██║"
        );

        printTerminal(
            "       ██║  ██╗███████╗██║  ██║██║ ╚████║███████╗██║"
        );

        printTerminal(
            "       ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═══╝╚══════╝╚═╝"
        );

        printTerminal("");

        printTerminal(
            "OS        KERNEL OS",
            "terminal-blue"
        );

        printTerminal(
            "VERSION   1.0.0"
        );

        printTerminal(
            "USER      guest"
        );

        printTerminal(
            "DOMAIN    MIT TECH KERNEL"
        );

        printTerminal(
            "STATUS    ONLINE",
            "terminal-green"
        );

        return;
    }


    /* KERNEL */

    if (cleanCommand === "kernel") {

        printTerminal(
            "Launching KERNEL member portal...",
            "terminal-yellow"
        );


        setTimeout(() => {

            window.location.href = "/login";

        }, 500);


        return;
    }


    /* CLEAR */

    if (cleanCommand === "clear") {

        terminalOutput.innerHTML = "";

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

            terminalInput.focus();

        }
    );

}


/* =========================================
   WINDOW RESIZE
========================================= */

window.addEventListener(
    "resize",
    function () {

        folders.forEach(folder => {

            const rect =
                folder.getBoundingClientRect();


            const desktopRect =
                desktop.getBoundingClientRect();


            const position =
                keepInsideDesktop(
                    folder,

                    rect.left -
                    desktopRect.left,

                    rect.top -
                    desktopRect.top
                );


            folder.style.left =
                `${position.x}px`;

            folder.style.top =
                `${position.y}px`;

        });


        savePositions();

    }
);
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
    ] = dateString
        .split("-")
        .map(Number);


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

    return parseCalendarDate(
        dateString
    ).getDate();

}


function getEventWeekday(dateString) {

    return parseCalendarDate(
        dateString
    ).toLocaleDateString(
        "en-US",
        {
            weekday: "short"
        }
    );

}


function getEventHeading(dateString) {

    return parseCalendarDate(
        dateString
    ).toLocaleDateString(
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
        getMonthName(
            calendarMonthDate
        );


    /* Clear */

    calendarEventsList.innerHTML = "";


    /* Filter events for current month */

    const monthEvents =
        calendarEvents.filter(event => {

            const date =
                parseCalendarDate(
                    event.date
                );


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


    /* =====================================
       CREATE DATE GROUPS
    ====================================== */

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
            getEventHeading(
                event.date
            );


        dateGroup.appendChild(
            heading
        );


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


        /* =================================
           CLICK EVENT
        ================================== */

        eventRow.addEventListener(
            "click",
            function () {

                const alreadySelected =
                    eventRow.classList.contains(
                        "selected"
                    );


                /* Close everything */

                document
                    .querySelectorAll(
                        ".calendar-event-row"
                    )
                    .forEach(row => {

                        row.classList.remove(
                            "selected"
                        );

                    });


                document
                    .querySelectorAll(
                        ".calendar-event-description"
                    )
                    .forEach(desc => {

                        desc.classList.remove(
                            "visible"
                        );

                    });


                /* Open selected */

                if (!alreadySelected) {

                    eventRow.classList.add(
                        "selected"
                    );

                    description.classList.add(
                        "visible"
                    );

                }

            }
        );


        dateGroup.appendChild(
            eventRow
        );


        dateGroup.appendChild(
            description
        );


        calendarEventsList.appendChild(
            dateGroup
        );

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