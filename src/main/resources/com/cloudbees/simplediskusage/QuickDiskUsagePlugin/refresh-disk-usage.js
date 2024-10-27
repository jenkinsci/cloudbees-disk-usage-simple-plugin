function refreshDiskUsage(a) {

    fetch("refresh", {
        method: "POST",
        headers: crumb.wrap({
            "Content-Type": "application/x-www-form-urlencoded",
        }),
    });

    hoverNotification('Refresh scheduled', a.parentNode);
}
