function refreshDiskUsage(a) {

    fetch("refresh", {
        method: "POST",
        headers: crumb.wrap({}),
    });

    hoverNotification('Refresh scheduled', a.parentNode);
}
