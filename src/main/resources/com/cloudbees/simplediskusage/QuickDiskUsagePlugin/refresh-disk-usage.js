function refreshDiskUsage(a) {

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "refresh", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    var headers = crumb.wrap({});
    xhr.setRequestHeader("Jenkins-Crumb", headers['Jenkins-Crumb']);

    xhr.send();

    hoverNotification('Refresh scheduled', a.parentNode);
}

//
// function refreshDiskUsage(a, event) {
//
//     console.log("Refreshing disk usage");
//
//     fetch("refresh", {
//         method: "POST",
//         headers: crumb.wrap({
//             "Content-Type": "application/x-www-form-urlencoded",
//         }),
//     });
//     event.preventDefault();
//
//     hoverNotification('Refresh scheduled', a.parentNode);
//     // return true;
// }
