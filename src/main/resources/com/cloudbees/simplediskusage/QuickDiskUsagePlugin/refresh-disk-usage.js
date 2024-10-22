function refreshDiskUsage(a) {

    fetch("refresh", {
        method: "POST",
        headers: crumb.wrap({})
    }).catch(() => {});

    hoverNotification('Refresh scheduled', a.parentNode);
    return true;
}
