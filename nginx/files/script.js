let user

const initialize = (webSocketServer, onOpen, onMessage) => {
    const ws = new WebSocket(`${webSocketServer}/ws`)

    ws.addEventListener("open", () => {
        onOpen()
    })

    ws.addEventListener("message", event => {
        onMessage(JSON.parse(event.data))
    })

    return ws
}

const onMessage = document => ({messageType, message}) => {
    if (messageType === "OneToOne") {
        appendToChatConsole(document, message.content)
    } else {

    }
}

const onOpen = document => () => appendToChatConsole(document, "Web Socket connection created")

const ws = initialize(`${(location.protocol === "https:" ? "wss" : "ws")}://${location.host}`, onOpen(document), onMessage(document))

const appendToChatConsole = (document, message) => {
    const block = document.createElement("div")
    block.innerText = message

    const chatConsole = document.getElementById("chat-console")
    chatConsole.appendChild(block)
}

const clickCreateUser = async () => {
    user = await userCreationFlow(document, ws)
}

const clickSendMessage = async () => {
    if (user != null) {
        sendMessage(user, document, ws)
    } else {
        console.error("Please create and authenticate user before sending any messages")
    }
}

const userCreationFlow = async (document, ws) => {
    const createdUser = await createUser(document)
    clearUserForm(document)

    const authenticationToken = await authenticate(createdUser.email, createdUser.password)

    const authenticationMessage =
        {messageType: "Authentication", message: {authenticationToken, messageId: Date.now().toString() }}

    ws.send(JSON.stringify(authenticationMessage))

    return createdUser
}

const sendMessage = (user, document, ws) => {
    const messageField = document.getElementById("message")
    const text = messageField.value

    const message = { messageType: "OneToOne", message: { messageId: Date.now().toString(), receiverId: user.id, content: text }}

    ws.send(JSON.stringify(message))
    messageField.value = ""
}

const clearUserForm = document => {
    ["first-name", "last-name", "email", "password"].forEach(id => {
        document.getElementById(id).value = ""
    })
}

const createUser = async document => {
    const firstName = document.getElementById("first-name").value
    const lastName = document.getElementById("last-name").value
    const email = document.getElementById("email").value
    const password = document.getElementById("password").value

    const response = await fetch(
        "/user", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({firstName, lastName, email, password})
        })

    const user = await response.json()

    return {...user, password}
}

const authenticate = async (email, password) => {
    const response = await fetch(
        "/authentication",
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({email, password})
        }
    )

    const {authenticationToken} = await response.json()

    return authenticationToken
}