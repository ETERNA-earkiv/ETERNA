document.addEventListener('DOMContentLoaded', () => {
    let footerProcessed = false;

    const observer = new MutationObserver(() => {
        if (footerProcessed) return;

        const footer = document.querySelector('.footer');
        if (footer) {
            footerProcessed = true;
            observer.disconnect();
            loadVersionInfo();
        }
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    async function loadVersionInfo() {
        try {
            const response = await fetch("api/v1/theme?resource_id=version.json");
            if (!response.ok) {
                throw new Error('Failed to load version.json');
            }
            const data = await response.json();
            if (data && data["git.build.version"]) {
                const versionDiv = document.querySelector('div#version');
                if (versionDiv) {
                    const versionElement = document.createElement('div');
                    versionElement.style.color = 'rgba(255, 255, 255, 0.5)';
                    versionElement.className = 'built_time';
                    versionElement.textContent = `Version ${data["git.build.version"]}`;
                    versionDiv.appendChild(versionElement);
                }
            }
        } catch (error) {
            console.warn("Failed to load version.json:", error);
        }
    }
});