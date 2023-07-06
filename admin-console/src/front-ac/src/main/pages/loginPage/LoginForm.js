import React, {useEffect, useState} from 'react';
import './LoginPage.css';
import axios from "../../../utils/axios";
import {getAccessToken, setAuth} from "../../../utils/auth";
import imageSrc from './ib.png';
import {useUserContext} from "../../../contexts/user-context"; // Import the image file

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const { login, logout } = useUserContext();

    useEffect(() => {
        //todo will probably fail when token expires
        const checkTokenAndLogin = async () =>{
        const token = getAccessToken();
            if (token) {
                await login()
            }
        }
        checkTokenAndLogin()
    },[])

    const handleEmailChange = (e) => {
        setEmail(e.target.value);
    };

    const handlePasswordChange = (e) => {
        setPassword(e.target.value);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        // Handle login logic here

        const response = await axios.post("http://localhost:8080/authenticate", {
            username: email,
            password: password
        })
        if (response?.data?.jwtToken) {
            await setAuth(response?.data?.jwtToken);
            await login()
        } else {
            logout()
        }
        // then((response) => {
        //     console.log("hoii")
        //     setLoggedIn(true);
        //     console.log(response)
        //     setAuth(response.jwtToken)
        // }).catch(() => {
        //     setLoggedIn(false)
        // })


        setEmail('');
        setPassword('');
    };

    return (
        <div className="loginCnt">
            <div className="loginCnt__img">
                <img src={imageSrc} alt="IB Mtiebi Logo" />
            </div>
            <div className="loginCnt__circleWrap">
                <div className="loginCnt__circle img"></div>

                <div className="loginCnt__circle blue">
                    <div className="formCnt">
                        <div className="formCnt__title">ავტორიზაცია</div>
                        <form onSubmit={handleSubmit}>
                            <input placeholder="ელ.ფოსტა" value={email} onChange={handleEmailChange} className="formCnt__input" type="text" />
                            <input placeholder="პაროლი" value={password} onChange={handlePasswordChange} className="formCnt__input" type="password" />
                            <div className="formCnt__btnCnt">
                                <button>შესვლა</button>
                            </div>
                        </form>
                    </div>
                </div>

                <div className="loginCnt__circle gray"></div>
                <div className="loginCnt__circle darkGray"></div>
                <div className="loginCnt__circle whiteOpacity"></div>
                <div className="loginCnt__circle purple"></div>

                <div className="loginCnt__footer">მოსწავლეთა შეფასების ჟურნალი</div>

            </div>
        </div>
    );
};

export default LoginPage;
