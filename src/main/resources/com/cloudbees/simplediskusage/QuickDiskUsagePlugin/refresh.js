function refresh(link) {

    fetch(link.href, {
        method: "POST",
        headers: crumb.wrap({})
    }).catch(() => {});

    hoverNotification('${%Refresh scheduled}', link);
    return true;
}
