import React, {useState} from 'react';
import './LoginPage.css';
import imageSrc from './ib.png';
import {useHistory} from "react-router-dom";
import {useUserContext} from "../../context/user-context";

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const history = useHistory(); // Add this line
    const {login} = useUserContext();

    const handleEmailChange = (e) => {
        setEmail(e.target.value);
    };

    const handlePasswordChange = (e) => {
        setPassword(e.target.value);
    };

    const handleSubmit = async (e) => {
        login({username: email, password: password});
        // const isLoggedIn = true;
        //
        // if (isLoggedIn) {
        //     setEmail('');
        //     setPassword('');
        //
        //     // Redirect to the AfterLoginPage
        //     history.push('/'); // This will redirect to the root route
        // }
    };

    return (
        <div className="loginCnt">
            <div className="loginCnt__img">
                <img src={imageSrc} alt="IB Mtiebi Logo"/>
            </div>
            <div className="loginCnt__circleWrap">
                <div className="loginCnt__circle img"></div>

                <div className="loginCnt__circle blue">
                    <div className="formCnt">
                        <div className="formCnt__title">ავტორიზაცია</div>
                        <form onSubmit={handleSubmit}>
                            <input placeholder="მომხმარებელი" value={email} onChange={handleEmailChange}
                                   className="formCnt__input" type="text"/>
                            <input placeholder="პაროლი" value={password} onChange={handlePasswordChange}
                                   className="formCnt__input" type="password"/>
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
