(() => {
    const eventSource = new EventSource('https://localhost:8443/api/test/event-stream?len=64')
    // const eventSource = new EventSource('http://localhost:8080/api/test/event-stream')
    eventSource.addEventListener('close', (event) => {
        console.log('event.type:', event.type)
        eventSource.close()
        for (let i = 0; i < 70; i++) {
            if (i < 5 || i > 65) {
                setTimeout(() => {
                    fetch('https://localhost:8443/api/ok')
                        .then(data => console.log(data))   // 处理数据
                        .catch(error => console.error('Error:', error)); // 错误处理
                }, 1000 + i * 1000)
            }
        }
    })
    eventSource.onopen = () => {
        document.body.innerHTML += '<ul id="event-messages"></ul>'
    }
    eventSource.onmessage = (event) => {
        // console.log('event.data:', event.data)
        // 将数据添加到页面
        const li = document.createElement('li')
        li.innerText = event.data
        document.getElementById("event-messages").appendChild(li)
    }
    eventSource.onerror = (error) => {
        console.error(error)
        eventSource.close()
    }
    eventSource.onclose = (obj) => {
        console.log('-- onclose --', obj)
    }
})()