function refreshDiskUsage(a) {

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "refresh", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    var headers = crumb.wrap({});
    xhr.setRequestHeader("Jenkins-Crumb", headers['Jenkins-Crumb']);

    xhr.send();

    hoverNotification('Refresh scheduled', a.parentNode);
}
