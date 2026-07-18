function refreshDiskUsage(a, ev) {
    fetch("refresh", {
        method: "post",
        headers: crumb.wrap({}),
    }).then((rsp) => {
        if (rsp.ok) {
            notificationBar.show("Refresh scheduled", notificationBar.SUCCESS);
        } else {
            notificationBar.show("Failed to schedule refresh", notificationBar.ERROR);
        }
    });
    ev.preventDefault();
}

Behaviour.specify("#refresh-disk-usage", "refresh-disk-usage", 0, function(button) {
    button.addEventListener("click", refreshDiskUsage);
});
