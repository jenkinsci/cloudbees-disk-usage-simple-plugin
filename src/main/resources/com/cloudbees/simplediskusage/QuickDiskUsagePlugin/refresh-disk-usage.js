function refreshDiskUsage(a, ev) {
    fetch("refresh", {
        method: "post",
        headers: crumb.wrap({}),
    }).then((rsp) => {
        if (rsp.ok) {
            hoverNotification("Refresh scheduled", a.parentNode);
        } else {
            hoverNotification("Failed to schedule refresh", a.parentNode);
        }
    });
    ev.preventDefault();
}
